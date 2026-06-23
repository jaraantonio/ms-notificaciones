package com.perfulandia.notificaciones.service;

import com.perfulandia.notificaciones.model.dto.NotificacionRequestDTO;
import com.perfulandia.notificaciones.model.dto.NotificacionResponseDTO;
import com.perfulandia.notificaciones.model.entity.Notificacion;
import com.perfulandia.notificaciones.model.enums.EstadoNotificacion;
import com.perfulandia.notificaciones.model.enums.TipoNotificacion;
import com.perfulandia.notificaciones.repository.NotificacionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Servicio principal de notificaciones.
 * Orquesta el flujo completo: guardar → renderizar → enviar (con reintentos) → actualizar estado.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final NotificacionRepository repository;
    private final EmailService emailService;

    private static final int MAX_REINTENTOS = 3;

    /**
     * Flujo completo de envío de correo con reintentos manuales (máx. 3).
     *
     * 1. Guarda registro con estado PENDIENTE
     * 2. Renderiza HTML con Thymeleaf según el tipo
     * 3. Si FACTURA_EMITIDA, genera PDF dummy adjunto
     * 4. Envía usando JavaMailSender
     * 5. Si falla: reintenta hasta 3 veces con backoff progresivo
     * 6. Si éxito: estado = ENVIADO, registra fechaEnvio
     * 7. Si 3 reintentos fallan: estado = FALLIDO, guarda mensaje de error
     */
    @Transactional
    public NotificacionResponseDTO enviarCorreo(NotificacionRequestDTO dto) {

        // 1. Determinar asunto (auto-generado si es null)
        String asunto = dto.asunto() != null
                ? dto.asunto()
                : generarAsunto(dto.tipo());

        // 2. Renderizar HTML con Thymeleaf
        String cuerpoHtml = emailService.renderizarHtml(dto.tipo(), dto.variables());

        // 3. Guardar registro PENDIENTE
        Notificacion notificacion = Notificacion.builder()
                .tipo(dto.tipo())
                .destinatario(dto.destinatario())
                .asunto(asunto)
                .cuerpo(cuerpoHtml)
                .estado(EstadoNotificacion.PENDIENTE)
                .intentos(0)
                .fechaCreacion(LocalDateTime.now())
                .build();

        notificacion = repository.save(notificacion);
        final Long notificacionId = notificacion.getId();
        log.info("Notificación #{} registrada — Tipo: {}, Destinatario: {}", notificacionId, dto.tipo(), dto.destinatario());

        // 4. Intentar envío con reintentos manuales
        Exception ultimaExcepcion = null;
        boolean enviado = false;

        for (int intento = 1; intento <= MAX_REINTENTOS && !enviado; intento++) {
            try {
                notificacion.setIntentos(intento);
                repository.save(notificacion);

                emailService.enviarCorreoHtml(notificacion);

                // Éxito
                notificacion.setEstado(EstadoNotificacion.ENVIADO);
                notificacion.setFechaEnvio(LocalDateTime.now());
                enviado = true;
                log.info("Notificación #{} enviada exitosamente (intento {}/{})", notificacionId, intento, MAX_REINTENTOS);

            } catch (Exception e) {
                ultimaExcepcion = e;
                log.warn("Notificación #{} — falló intento {}/{}: {}", notificacionId, intento, MAX_REINTENTOS, e.getMessage());

                if (intento < MAX_REINTENTOS) {
                    // Backoff progresivo: 1s, 2s, 3s
                    try {
                        long backoff = intento * 1000L;
                        log.debug("Esperando {} ms antes del reintento...", backoff);
                        Thread.sleep(backoff);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("Backoff interrumpido para notificación #{}", notificacionId);
                        break;
                    }
                }
            }
        }

        // 5. Si todos los intentos fallaron
        if (!enviado) {
            notificacion.setEstado(EstadoNotificacion.FALLIDO);
            notificacion.setIntentos(MAX_REINTENTOS);
            String mensajeError = ultimaExcepcion != null
                    ? ultimaExcepcion.getClass().getSimpleName() + ": " + ultimaExcepcion.getMessage()
                    : "Error desconocido tras " + MAX_REINTENTOS + " reintentos";
            notificacion.setError(mensajeError);
            log.error("Notificación #{} FALLIDA tras {} reintentos: {}", notificacionId, MAX_REINTENTOS, mensajeError);
        }

        notificacion = repository.save(notificacion);
        return mapearAResponse(notificacion);
    }

    /**
     * Consulta paginada de logs con filtros opcionales por tipo y/o estado.
     * Si ambos filtros son null, devuelve todos los registros.
     */
    @Transactional(readOnly = true)
    public Page<NotificacionResponseDTO> obtenerLogs(Pageable pageable, String tipo, String estado) {
        TipoNotificacion tipoEnum = tipo != null ? TipoNotificacion.valueOf(tipo.toUpperCase()) : null;
        EstadoNotificacion estadoEnum = estado != null ? EstadoNotificacion.valueOf(estado.toUpperCase()) : null;

        Page<Notificacion> pagina;
        if (tipoEnum != null && estadoEnum != null) {
            pagina = repository.findByTipoAndEstado(tipoEnum, estadoEnum, pageable);
        } else if (tipoEnum != null) {
            pagina = repository.findByTipo(tipoEnum, pageable);
        } else if (estadoEnum != null) {
            pagina = repository.findByEstado(estadoEnum, pageable);
        } else {
            pagina = repository.findAll(pageable);
        }

        return pagina.map(this::mapearAResponse);
    }

    // ─── Métodos auxiliares ───

    /**
     * Genera un asunto automático basado en el tipo de notificación.
     */
    private String generarAsunto(TipoNotificacion tipo) {
        return switch (tipo) {
            case CONFIRMACION_PAGO   -> "Confirmación de Pago — Perfulandia SPA";
            case ACTUALIZACION_ENVIO -> "Actualización de Envío — Perfulandia SPA";
            case FACTURA_EMITIDA     -> "Factura Emitida — Perfulandia SPA";
            case RECUPERACION_CLAVE  -> "Recuperación de Clave — Perfulandia SPA";
            case ALERTA_SISTEMA      -> "Alerta del Sistema — Perfulandia SPA";
        };
    }

    /**
     * Convierte una entidad Notificacion a DTO de respuesta.
     */
    private NotificacionResponseDTO mapearAResponse(Notificacion n) {
        return new NotificacionResponseDTO(
                n.getId(),
                n.getTipo(),
                n.getDestinatario(),
                n.getAsunto(),
                n.getEstado(),
                n.getIntentos() != null ? n.getIntentos() : 0,
                n.getFechaCreacion(),
                n.getFechaEnvio(),
                n.getError()
        );
    }
}
