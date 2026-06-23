package com.perfulandia.notificaciones.service;

import com.perfulandia.notificaciones.model.entity.Notificacion;
import com.perfulandia.notificaciones.model.enums.TipoNotificacion;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Servicio encargado únicamente del envío de correos electrónicos.
 * Orquesta JavaMailSender + Thymeleaf para generar y despachar emails HTML.
 * Para FACTURA_EMITIDA genera un PDF dummy como adjunto.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    /**
     * Envía un correo HTML usando la plantilla Thymeleaf correspondiente al tipo de notificación.
     *
     * @param notificacion entidad con destinatario, asunto y cuerpo ya renderizado
     * @throws jakarta.mail.MessagingException si falla el envío
     */
    public void enviarCorreoHtml(Notificacion notificacion) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        // multipart = true para permitir adjuntos
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(notificacion.getDestinatario());
        helper.setSubject(notificacion.getAsunto());
        helper.setText(notificacion.getCuerpo(), true);

        // Adjuntar PDF dummy si es FACTURA_EMITIDA
        if (notificacion.getTipo() == TipoNotificacion.FACTURA_EMITIDA) {
            byte[] pdfBytes = generarPdfDummy();
            helper.addAttachment("factura.pdf", new ByteArrayDataSource(pdfBytes, "application/pdf"));
            log.debug("PDF dummy adjuntado para notificación ID {}", notificacion.getId());
        }

        mailSender.send(message);
        log.info("Correo enviado exitosamente a {} — Tipo: {}", notificacion.getDestinatario(), notificacion.getTipo());
    }

    /**
     * Renderiza el HTML combinando la plantilla Thymeleaf con las variables de contexto.
     *
     * @param tipo      tipo de notificación (determina la plantilla)
     * @param variables mapa de variables para el contexto Thymeleaf
     * @return HTML renderizado listo para enviar
     */
    public String renderizarHtml(TipoNotificacion tipo, Map<String, Object> variables) {
        String template = getTemplateName(tipo);
        Context context = new Context();
        context.setVariables(variables);
        return templateEngine.process(template, context);
    }

    /**
     * Determina el nombre de la plantilla Thymeleaf según el tipo de notificación.
     * FACTURA_EMITIDA reutiliza confirmacion-pago + adjunto PDF.
     */
    private String getTemplateName(TipoNotificacion tipo) {
        return switch (tipo) {
            case CONFIRMACION_PAGO  -> "confirmacion-pago";
            case ACTUALIZACION_ENVIO -> "actualizacion-envio";
            case FACTURA_EMITIDA    -> "confirmacion-pago";  // misma plantilla, se añade PDF
            case RECUPERACION_CLAVE -> "recuperacion-clave";
            case ALERTA_SISTEMA     -> "alerta-sistema";
        };
    }

    /**
     * Genera un archivo PDF mínimo válido (dummy) para adjuntar a facturas.
     * En un entorno productivo se integraría una librería como iText o JasperReports.
     */
    private byte[] generarPdfDummy() {
        String minimalPdf = """
                %PDF-1.4
                1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj
                2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj
                3 0 obj<</Type/Page/MediaBox[0 0 612 792]/Parent 2 0 R/Resources<<>>>>endobj
                xref
                0 4
                0000000000 65535 f\s
                0000000009 00000 n\s
                0000000058 00000 n\s
                0000000115 00000 n\s
                trailer<</Size 4/Root 1 0 R>>
                startxref
                190
                %%EOF""";
        return minimalPdf.getBytes(StandardCharsets.ISO_8859_1);
    }
}
