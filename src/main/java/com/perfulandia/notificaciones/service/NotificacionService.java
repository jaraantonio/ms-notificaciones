package com.perfulandia.notificaciones.service;

import java.time.LocalDateTime;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.perfulandia.notificaciones.client.UsuarioClient;
import com.perfulandia.notificaciones.model.dto.NotificacionRequestDTO;
import com.perfulandia.notificaciones.model.dto.NotificacionResponseDTO;
import com.perfulandia.notificaciones.model.entity.Notificacion;
import com.perfulandia.notificaciones.model.enums.EstadoNotificacion;
import com.perfulandia.notificaciones.repository.NotificacionRepository;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final NotificacionRepository repository;
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final UsuarioClient usuarioClient;

    @Transactional
    public NotificacionResponseDTO procesarNotificacion(NotificacionRequestDTO request) {
        Notificacion notificacion = Notificacion.builder()
                .idUsuario(request.idUsuario())
                .destinatario(request.destinatario())
                .asunto(request.asunto())
                .cuerpoMensaje(request.cuerpoMensaje())
                .tipo(request.tipo())
                .estado(EstadoNotificacion.PENDIENTE)
                .build();

        notificacion = repository.save(notificacion);

        try {
            enviarEmailHTML(notificacion);
            notificacion.setEstado(EstadoNotificacion.ENVIADA);
            notificacion.setFechaEnvio(LocalDateTime.now());
            log.info("Notificación {} enviada exitosamente a {}", notificacion.getIdNotificacion(),
                    notificacion.getDestinatario());
        } catch (Exception e) {
            notificacion.setEstado(EstadoNotificacion.FALLIDA);
            log.error("Fallo al enviar la notificación {}: {}", notificacion.getIdNotificacion(), e.getMessage());
        }

        notificacion = repository.save(notificacion);
        return mapearAResponse(notificacion);
    }

    private void enviarEmailHTML(Notificacion notificacion) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        Context context = new Context();
        context.setVariable("asunto", notificacion.getAsunto());
        context.setVariable("cuerpoMensaje", notificacion.getCuerpoMensaje());

        String html = templateEngine.process("email-template", context);

        helper.setTo(notificacion.getDestinatario());
        helper.setSubject(notificacion.getAsunto());
        helper.setText(html, true);

        mailSender.send(message);
    }

    public boolean monitorearAlertasDelSistema() {
        try {
            usuarioClient.checkHealth();
            log.info("ms-usuarios responde correctamente (UP)");
            return true;
        } catch (Exception e) {
            log.error("ms-usuarios está CAÍDO o no responde. Gatillando alerta de prioridad alta.");
            gatillarAlertaAdministrador();
            return false;
        }
    }

    private void gatillarAlertaAdministrador() {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setTo("admin@perfulandia.cl");
            helper.setSubject("ALERTA CRÍTICA: Microservicio Caído");
            helper.setText(
                    "El microservicio ms-usuarios no responde en el puerto 8081. Se requiere intervención técnica inmediata.");
            mailSender.send(message);
            log.info("Correo de alerta crítica enviado al administrador.");
        } catch (Exception mailEx) {
            log.error("Fallo crítico: No se pudo enviar el correo de alerta al admin. Causa: {}", mailEx.getMessage());
        }
    }

    private NotificacionResponseDTO mapearAResponse(Notificacion notificacion) {
        return new NotificacionResponseDTO(
                notificacion.getIdNotificacion(),
                notificacion.getIdUsuario(),
                notificacion.getDestinatario(),
                notificacion.getAsunto(),
                notificacion.getCuerpoMensaje(),
                notificacion.getTipo(),
                notificacion.getEstado(),
                notificacion.getFechaEnvio());
    }
}