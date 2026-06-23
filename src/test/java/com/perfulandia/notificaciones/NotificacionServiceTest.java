package com.perfulandia.notificaciones;

import com.perfulandia.notificaciones.model.dto.NotificacionRequestDTO;
import com.perfulandia.notificaciones.model.dto.NotificacionResponseDTO;
import com.perfulandia.notificaciones.model.entity.Notificacion;
import com.perfulandia.notificaciones.model.enums.EstadoNotificacion;
import com.perfulandia.notificaciones.model.enums.TipoNotificacion;
import com.perfulandia.notificaciones.repository.NotificacionRepository;
import com.perfulandia.notificaciones.service.EmailService;
import com.perfulandia.notificaciones.service.NotificacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificacionService — Pruebas unitarias")
class NotificacionServiceTest {

    @Mock
    private NotificacionRepository repository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private NotificacionService notificacionService;

    private NotificacionRequestDTO requestValida;

    @BeforeEach
    void setUp() {
        requestValida = new NotificacionRequestDTO(
                TipoNotificacion.CONFIRMACION_PAGO,
                "cliente@email.com",
                null, // asunto auto-generado
                Map.of("nombre", "Juan", "monto", "$45.990", "pedidoId", "#P-001", "fecha", "15/01/2026"),
                null
        );
    }

    // ═══════════════════════════════════════════════════════════
    // Caso 1: Envío exitoso al primer intento
    // ═══════════════════════════════════════════════════════════
    @Test
    @DisplayName("Caso 1 — Envío exitoso al primer intento")
    void enviarCorreo_Exitoso_PrimerIntento() throws Exception {
        // Given
        when(emailService.renderizarHtml(eq(TipoNotificacion.CONFIRMACION_PAGO), anyMap()))
                .thenReturn("<html>Confirmación de pago</html>");
        when(repository.save(any(Notificacion.class))).thenAnswer(inv -> {
            Notificacion n = inv.getArgument(0);
            if (n.getId() == null) n.setId(1L);
            return n;
        });
        doNothing().when(emailService).enviarCorreoHtml(any(Notificacion.class));

        // When
        NotificacionResponseDTO response = notificacionService.enviarCorreo(requestValida);

        // Then
        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals(EstadoNotificacion.ENVIADO, response.estado());
        assertEquals("Confirmación de Pago — Perfulandia SPA", response.asunto());
        assertEquals(1, response.intentos());
        assertNotNull(response.fechaEnvio());
        assertNull(response.error());

        verify(emailService, times(1)).enviarCorreoHtml(any(Notificacion.class));
        verify(repository, atLeast(2)).save(any(Notificacion.class)); // save PENDIENTE + save ENVIADO
    }

    // ═══════════════════════════════════════════════════════════
    // Caso 2: 3 reintentos fallidos → FALLIDO
    // ═══════════════════════════════════════════════════════════
    @Test
    @DisplayName("Caso 2 — 3 reintentos fallidos, estado final FALLIDO")
    void enviarCorreo_TresReintentosFallidos_EstadoFallido() throws Exception {
        // Given
        when(emailService.renderizarHtml(any(), anyMap())).thenReturn("<html>...</html>");
        when(repository.save(any(Notificacion.class))).thenAnswer(inv -> {
            Notificacion n = inv.getArgument(0);
            if (n.getId() == null) n.setId(2L);
            return n;
        });
        doThrow(new RuntimeException("Error SMTP"))
                .when(emailService).enviarCorreoHtml(any(Notificacion.class));

        // When
        NotificacionResponseDTO response = notificacionService.enviarCorreo(requestValida);

        // Then
        assertEquals(EstadoNotificacion.FALLIDO, response.estado());
        assertEquals(3, response.intentos());
        assertNull(response.fechaEnvio());
        assertNotNull(response.error());
        assertTrue(response.error().contains("Error SMTP"));

        // Verificar que se intentó 3 veces
        verify(emailService, times(3)).enviarCorreoHtml(any(Notificacion.class));
    }

    // ═══════════════════════════════════════════════════════════
    // Caso 3: Éxito en el segundo intento
    // ═══════════════════════════════════════════════════════════
    @Test
    @DisplayName("Caso 3 — Éxito en el segundo intento")
    void enviarCorreo_Exito_SegundoIntento() throws Exception {
        // Given
        when(emailService.renderizarHtml(any(), anyMap())).thenReturn("<html>...</html>");
        when(repository.save(any(Notificacion.class))).thenAnswer(inv -> {
            Notificacion n = inv.getArgument(0);
            if (n.getId() == null) n.setId(3L);
            return n;
        });
        // Falla en el primer intento, éxito en el segundo
        doThrow(new RuntimeException("Fallo temporal"))
                .doNothing()
                .when(emailService).enviarCorreoHtml(any(Notificacion.class));

        // When
        NotificacionResponseDTO response = notificacionService.enviarCorreo(requestValida);

        // Then
        assertEquals(EstadoNotificacion.ENVIADO, response.estado());
        assertEquals(2, response.intentos());
        assertNotNull(response.fechaEnvio());

        verify(emailService, times(2)).enviarCorreoHtml(any(Notificacion.class));
    }

    // ═══════════════════════════════════════════════════════════
    // Caso 4: FACTURA_EMITIDA con adjunto
    // ═══════════════════════════════════════════════════════════
    @Test
    @DisplayName("Caso 4 — FACTURA_EMITIDA con PDF adjunto")
    void enviarCorreo_FacturaEmitida_ConAdjunto() throws Exception {
        // Given
        NotificacionRequestDTO facturaRequest = new NotificacionRequestDTO(
                TipoNotificacion.FACTURA_EMITIDA,
                "empresa@cliente.cl",
                "Factura #F-001",
                Map.of("nombre", "Empresa Ltda", "monto", "$890.000", "pedidoId", "#F-001", "fecha", "20/01/2026"),
                null // archivoAdjunto lo genera el EmailService
        );

        when(emailService.renderizarHtml(eq(TipoNotificacion.FACTURA_EMITIDA), anyMap()))
                .thenReturn("<html>Factura</html>");
        when(repository.save(any(Notificacion.class))).thenAnswer(inv -> {
            Notificacion n = inv.getArgument(0);
            if (n.getId() == null) n.setId(4L);
            return n;
        });
        doNothing().when(emailService).enviarCorreoHtml(any(Notificacion.class));

        // When
        NotificacionResponseDTO response = notificacionService.enviarCorreo(facturaRequest);

        // Then
        assertEquals(TipoNotificacion.FACTURA_EMITIDA, response.tipo());
        assertEquals(EstadoNotificacion.ENVIADO, response.estado());
        assertEquals("Factura #F-001", response.asunto());
        verify(emailService, times(1)).enviarCorreoHtml(any(Notificacion.class));
    }

    // ═══════════════════════════════════════════════════════════
    // Caso 5: Auto-generación del asunto cuando es null
    // ═══════════════════════════════════════════════════════════
    @Test
    @DisplayName("Caso 5 — Asunto auto-generado cuando es null")
    void enviarCorreo_AsuntoNull_AutoGenerado() throws Exception {
        // Given
        when(emailService.renderizarHtml(any(), anyMap())).thenReturn("<html>...</html>");
        when(repository.save(any(Notificacion.class))).thenAnswer(inv -> {
            Notificacion n = inv.getArgument(0);
            if (n.getId() == null) n.setId(5L);
            return n;
        });
        doNothing().when(emailService).enviarCorreoHtml(any(Notificacion.class));

        // When
        NotificacionResponseDTO response = notificacionService.enviarCorreo(requestValida);

        // Then
        assertEquals("Confirmación de Pago — Perfulandia SPA", response.asunto());
    }

    // ═══════════════════════════════════════════════════════════
    // Caso 6: obtenerLogs — sin filtros
    // ═══════════════════════════════════════════════════════════
    @Test
    @DisplayName("Caso 6 — Obtener logs sin filtros (paginado)")
    void obtenerLogs_SinFiltros_Paginado() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Notificacion> notificaciones = List.of(
                Notificacion.builder().id(1L).tipo(TipoNotificacion.CONFIRMACION_PAGO)
                        .destinatario("a@b.cl").asunto("Test")
                        .cuerpo("...").estado(EstadoNotificacion.ENVIADO)
                        .intentos(1).fechaCreacion(LocalDateTime.now()).build()
        );
        Page<Notificacion> pagina = new PageImpl<>(notificaciones, pageable, 1);
        when(repository.findAll(pageable)).thenReturn(pagina);

        // When
        Page<NotificacionResponseDTO> result = notificacionService.obtenerLogs(pageable, null, null);

        // Then
        assertEquals(1, result.getTotalElements());
        assertEquals(EstadoNotificacion.ENVIADO, result.getContent().get(0).estado());
        verify(repository, times(1)).findAll(pageable);
    }

    // ═══════════════════════════════════════════════════════════
    // Caso 7: obtenerLogs — con filtro por tipo
    // ═══════════════════════════════════════════════════════════
    @Test
    @DisplayName("Caso 7 — Obtener logs filtrados por tipo")
    void obtenerLogs_FiltroTipo() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notificacion> paginaVacia = Page.empty();
        when(repository.findByTipo(eq(TipoNotificacion.ALERTA_SISTEMA), any(Pageable.class)))
                .thenReturn(paginaVacia);

        // When
        Page<NotificacionResponseDTO> result = notificacionService.obtenerLogs(pageable, "ALERTA_SISTEMA", null);

        // Then
        assertEquals(0, result.getTotalElements());
        verify(repository, times(1)).findByTipo(eq(TipoNotificacion.ALERTA_SISTEMA), any(Pageable.class));
    }

    // ═══════════════════════════════════════════════════════════
    // Caso 8: obtenerLogs — con filtro por estado
    // ═══════════════════════════════════════════════════════════
    @Test
    @DisplayName("Caso 8 — Obtener logs filtrados por estado")
    void obtenerLogs_FiltroEstado() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Notificacion> fallidas = List.of(
                Notificacion.builder().id(6L).tipo(TipoNotificacion.FACTURA_EMITIDA)
                        .destinatario("x@y.cl").asunto("Fallo").cuerpo("...")
                        .estado(EstadoNotificacion.FALLIDO).intentos(3)
                        .fechaCreacion(LocalDateTime.now())
                        .error("Error SMTP").build()
        );
        Page<Notificacion> paginaFallidas = new PageImpl<>(fallidas, pageable, 1);
        when(repository.findByEstado(eq(EstadoNotificacion.FALLIDO), any(Pageable.class)))
                .thenReturn(paginaFallidas);

        // When
        Page<NotificacionResponseDTO> result = notificacionService.obtenerLogs(pageable, null, "FALLIDO");

        // Then
        assertEquals(1, result.getTotalElements());
        assertEquals(EstadoNotificacion.FALLIDO, result.getContent().get(0).estado());
        assertEquals(3, result.getContent().get(0).intentos());
        assertNotNull(result.getContent().get(0).error());
    }
}
