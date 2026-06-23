package com.perfulandia.notificaciones;

import com.perfulandia.notificaciones.model.entity.Notificacion;
import com.perfulandia.notificaciones.model.enums.EstadoNotificacion;
import com.perfulandia.notificaciones.model.enums.TipoNotificacion;
import com.perfulandia.notificaciones.service.EmailService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("EmailService — Pruebas unitarias")
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @InjectMocks
    private EmailService emailService;

    private Notificacion notificacion;
    private final Session mailSession = Session.getInstance(new Properties());

    @BeforeEach
    void setUp() {
        notificacion = Notificacion.builder()
                .id(1L)
                .tipo(TipoNotificacion.CONFIRMACION_PAGO)
                .destinatario("cliente@email.com")
                .asunto("Confirmación de Pago — Perfulandia SPA")
                .cuerpo("<html><body>Gracias por tu compra</body></html>")
                .estado(EstadoNotificacion.PENDIENTE)
                .intentos(0)
                .fechaCreacion(LocalDateTime.now())
                .build();

        lenient().when(mailSender.createMimeMessage())
                .thenReturn(new MimeMessage(mailSession));
    }

    @Test
    @DisplayName("renderizarHtml — CONFIRMACION_PAGO usa plantilla correcta")
    void renderizarHtml_ConfirmacionPago() {
        Map<String, Object> variables = Map.of(
                "nombre", "Juan", "monto", "$45.990",
                "pedidoId", "#P-001", "fecha", "15/01/2026"
        );

        when(templateEngine.process(eq("confirmacion-pago"), any(Context.class)))
                .thenReturn("<html>Confirmación renderizada</html>");

        String html = emailService.renderizarHtml(TipoNotificacion.CONFIRMACION_PAGO, variables);

        assertNotNull(html);
        assertTrue(html.contains("Confirmación renderizada"));
        verify(templateEngine).process(eq("confirmacion-pago"), any(Context.class));
    }

    @Test
    @DisplayName("renderizarHtml — ACTUALIZACION_ENVIO usa plantilla correcta")
    void renderizarHtml_ActualizacionEnvio() {
        Map<String, Object> variables = Map.of(
                "nombre", "Juan", "pedidoId", "#E-101",
                "estadoEnvio", "EN CAMINO",
                "linkSeguimiento", "https://perfulandia.cl/seguimiento/E-101"
        );

        when(templateEngine.process(eq("actualizacion-envio"), any(Context.class)))
                .thenReturn("<html>Envío actualizado</html>");

        String html = emailService.renderizarHtml(TipoNotificacion.ACTUALIZACION_ENVIO, variables);
        assertNotNull(html);
        verify(templateEngine).process(eq("actualizacion-envio"), any(Context.class));
    }

    @Test
    @DisplayName("renderizarHtml — RECUPERACION_CLAVE usa plantilla correcta")
    void renderizarHtml_RecuperacionClave() {
        Map<String, Object> variables = Map.of(
                "nombre", "Pedro",
                "enlaceRestablecimiento", "https://perfulandia.cl/restablecer?token=abc"
        );

        when(templateEngine.process(eq("recuperacion-clave"), any(Context.class)))
                .thenReturn("<html>Recuperación</html>");

        String html = emailService.renderizarHtml(TipoNotificacion.RECUPERACION_CLAVE, variables);
        assertNotNull(html);
        verify(templateEngine).process(eq("recuperacion-clave"), any(Context.class));
    }

    @Test
    @DisplayName("renderizarHtml — ALERTA_SISTEMA usa plantilla correcta")
    void renderizarHtml_AlertaSistema() {
        Map<String, Object> variables = Map.of(
                "nombreMicroservicio", "ms-pagos",
                "error", "Connection refused",
                "timestamp", "2026-02-10T22:15:00"
        );

        when(templateEngine.process(eq("alerta-sistema"), any(Context.class)))
                .thenReturn("<html>Alerta</html>");

        String html = emailService.renderizarHtml(TipoNotificacion.ALERTA_SISTEMA, variables);
        assertNotNull(html);
        verify(templateEngine).process(eq("alerta-sistema"), any(Context.class));
    }

    @Test
    @DisplayName("renderizarHtml — FACTURA_EMITIDA reutiliza confirmacion-pago")
    void renderizarHtml_FacturaEmitida() {
        Map<String, Object> variables = Map.of(
                "nombre", "Empresa", "monto", "$890.000",
                "pedidoId", "#F-050", "fecha", "20/01/2026"
        );

        when(templateEngine.process(eq("confirmacion-pago"), any(Context.class)))
                .thenReturn("<html>Factura</html>");

        String html = emailService.renderizarHtml(TipoNotificacion.FACTURA_EMITIDA, variables);
        assertNotNull(html);
        verify(templateEngine).process(eq("confirmacion-pago"), any(Context.class));
    }

    @Test
    @DisplayName("enviarCorreoHtml — Envío exitoso sin adjunto")
    void enviarCorreoHtml_Exitoso_SinAdjunto() throws Exception {
        emailService.enviarCorreoHtml(notificacion);
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("enviarCorreoHtml — FACTURA_EMITIDA con PDF adjunto")
    void enviarCorreoHtml_FacturaEmitida_ConAdjunto() throws Exception {
        notificacion.setTipo(TipoNotificacion.FACTURA_EMITIDA);

        emailService.enviarCorreoHtml(notificacion);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage sent = captor.getValue();
        assertNotNull(sent);
    }
}
