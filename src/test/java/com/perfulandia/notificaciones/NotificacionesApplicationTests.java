package com.perfulandia.notificaciones;

import com.perfulandia.notificaciones.exception.EmailSendException;
import com.perfulandia.notificaciones.exception.NotificacionNotFoundException;
import com.perfulandia.notificaciones.model.dto.ErrorResponseDTO;
import com.perfulandia.notificaciones.model.dto.NotificacionRequestDTO;
import com.perfulandia.notificaciones.model.dto.NotificacionResponseDTO;
import com.perfulandia.notificaciones.model.entity.Notificacion;
import com.perfulandia.notificaciones.model.enums.EstadoNotificacion;
import com.perfulandia.notificaciones.model.enums.TipoNotificacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("NotificacionesApplication — Context load + enums + exceptions + entity + DTOs")
class NotificacionesApplicationTests {

    @Test
    @DisplayName("Contexto de Spring carga correctamente")
    void contextLoads() {
    }

    @Test
    @DisplayName("TipoNotificacion — todos los valores definidos")
    void testTipoNotificacion_Valores() {
        TipoNotificacion[] tipos = TipoNotificacion.values();
        assertEquals(5, tipos.length);
        assertNotNull(TipoNotificacion.valueOf("CONFIRMACION_PAGO"));
        assertNotNull(TipoNotificacion.valueOf("ACTUALIZACION_ENVIO"));
        assertNotNull(TipoNotificacion.valueOf("FACTURA_EMITIDA"));
        assertNotNull(TipoNotificacion.valueOf("RECUPERACION_CLAVE"));
        assertNotNull(TipoNotificacion.valueOf("ALERTA_SISTEMA"));
    }

    @Test
    @DisplayName("TipoNotificacion — ordinal y name correctos")
    void testTipoNotificacion_OrdinalYName() {
        assertEquals("CONFIRMACION_PAGO", TipoNotificacion.CONFIRMACION_PAGO.name());
        assertEquals("ACTUALIZACION_ENVIO", TipoNotificacion.ACTUALIZACION_ENVIO.name());
    }

    @Test
    @DisplayName("EstadoNotificacion — todos los valores definidos")
    void testEstadoNotificacion_Valores() {
        EstadoNotificacion[] estados = EstadoNotificacion.values();
        assertEquals(3, estados.length);
        assertNotNull(EstadoNotificacion.valueOf("PENDIENTE"));
        assertNotNull(EstadoNotificacion.valueOf("ENVIADO"));
        assertNotNull(EstadoNotificacion.valueOf("FALLIDO"));
    }

    @Test
    @DisplayName("EstadoNotificacion — ciclo de vida esperado")
    void testEstadoNotificacion_CicloVida() {
        assertEquals("PENDIENTE", EstadoNotificacion.PENDIENTE.name());
        assertEquals("ENVIADO", EstadoNotificacion.ENVIADO.name());
        assertEquals("FALLIDO", EstadoNotificacion.FALLIDO.name());
    }

    @Test
    @DisplayName("EmailSendException — constructor solo mensaje")
    void testEmailSendException_SoloMensaje() {
        EmailSendException ex = new EmailSendException("Error SMTP");
        assertEquals("Error SMTP", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    @DisplayName("EmailSendException — constructor con causa")
    void testEmailSendException_ConCausa() {
        RuntimeException cause = new RuntimeException("Connection refused");
        EmailSendException ex = new EmailSendException("Error al enviar", cause);
        assertEquals("Error al enviar", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }

    @Test
    @DisplayName("EmailSendException — extendida de RuntimeException")
    void testEmailSendException_EsRuntimeException() {
        EmailSendException ex = new EmailSendException("test");
        assertInstanceOf(RuntimeException.class, ex);
    }

    @Test
    @DisplayName("NotificacionNotFoundException — mensaje contiene ID")
    void testNotificacionNotFoundException_Constructor() {
        NotificacionNotFoundException ex = new NotificacionNotFoundException(99L);
        assertTrue(ex.getMessage().contains("99"));
        assertTrue(ex.getMessage().contains("Notificación no encontrada"));
    }

    @Test
    @DisplayName("NotificacionNotFoundException — con ID 0")
    void testNotificacionNotFoundException_IdCero() {
        NotificacionNotFoundException ex = new NotificacionNotFoundException(0L);
        assertTrue(ex.getMessage().contains("0"));
    }

    @Test
    @DisplayName("NotificacionNotFoundException — extendida de RuntimeException")
    void testNotificacionNotFoundException_EsRuntimeException() {
        NotificacionNotFoundException ex = new NotificacionNotFoundException(1L);
        assertInstanceOf(RuntimeException.class, ex);
    }

    @Test
    @DisplayName("Notificacion @Builder crea entidad con todos los campos")
    void testNotificacion_Builder_CreaEntidadCompleta() {
        LocalDateTime now = LocalDateTime.now();
        Notificacion n = Notificacion.builder()
                .id(1L)
                .tipo(TipoNotificacion.CONFIRMACION_PAGO)
                .destinatario("cliente@email.com")
                .asunto("Confirmación de Pago")
                .cuerpo("<html>Gracias</html>")
                .estado(EstadoNotificacion.ENVIADO)
                .intentos(2)
                .fechaCreacion(now)
                .fechaEnvio(now)
                .error("algún error")
                .build();

        assertEquals(1L, n.getId());
        assertEquals(TipoNotificacion.CONFIRMACION_PAGO, n.getTipo());
        assertEquals("cliente@email.com", n.getDestinatario());
        assertEquals("Confirmación de Pago", n.getAsunto());
        assertEquals("<html>Gracias</html>", n.getCuerpo());
        assertEquals(EstadoNotificacion.ENVIADO, n.getEstado());
        assertEquals(Integer.valueOf(2), n.getIntentos());
        assertEquals(now, n.getFechaCreacion());
        assertEquals(now, n.getFechaEnvio());
        assertEquals("algún error", n.getError());
    }

    @Test
    @DisplayName("Notificacion @NoArgsConstructor con valores default")
    void testNotificacion_NoArgsConstructor_ValoresDefault() {
        Notificacion n = new Notificacion();
        assertNull(n.getId());
        assertNull(n.getTipo());
        assertNull(n.getDestinatario());
        assertNull(n.getAsunto());
        assertNull(n.getCuerpo());
        assertEquals(EstadoNotificacion.PENDIENTE, n.getEstado());
        assertEquals(Integer.valueOf(0), n.getIntentos());
        assertNotNull(n.getFechaCreacion());
        assertNull(n.getFechaEnvio());
        assertNull(n.getError());
    }

    @Test
    @DisplayName("Notificacion @AllArgsConstructor establece todos los campos")
    void testNotificacion_AllArgsConstructor_EstableceCampos() {
        LocalDateTime now = LocalDateTime.now();
        Notificacion n = new Notificacion(
                10L,
                TipoNotificacion.ALERTA_SISTEMA,
                "admin@perfulandia.cl",
                "Alerta del Sistema",
                "<html>Alerta</html>",
                EstadoNotificacion.FALLIDO,
                3,
                now,
                now,
                "Timeout"
        );

        assertEquals(10L, n.getId());
        assertEquals(TipoNotificacion.ALERTA_SISTEMA, n.getTipo());
        assertEquals("admin@perfulandia.cl", n.getDestinatario());
        assertEquals("Alerta del Sistema", n.getAsunto());
        assertEquals("<html>Alerta</html>", n.getCuerpo());
        assertEquals(EstadoNotificacion.FALLIDO, n.getEstado());
        assertEquals(Integer.valueOf(3), n.getIntentos());
        assertEquals(now, n.getFechaCreacion());
        assertEquals(now, n.getFechaEnvio());
        assertEquals("Timeout", n.getError());
    }

    @Test
    @DisplayName("Notificacion Getters y Setters funcionan")
    void testNotificacion_GettersSetters() {
        Notificacion n = new Notificacion();
        LocalDateTime now = LocalDateTime.now();

        n.setId(42L);
        n.setTipo(TipoNotificacion.FACTURA_EMITIDA);
        n.setDestinatario("factura@empresa.cl");
        n.setAsunto("Factura #F-001");
        n.setCuerpo("<html>Factura</html>");
        n.setEstado(EstadoNotificacion.PENDIENTE);
        n.setIntentos(0);
        n.setFechaCreacion(now);
        n.setFechaEnvio(now);
        n.setError("test error");

        assertEquals(42L, n.getId());
        assertEquals(TipoNotificacion.FACTURA_EMITIDA, n.getTipo());
        assertEquals("factura@empresa.cl", n.getDestinatario());
        assertEquals("Factura #F-001", n.getAsunto());
        assertEquals("<html>Factura</html>", n.getCuerpo());
        assertEquals(EstadoNotificacion.PENDIENTE, n.getEstado());
        assertEquals(Integer.valueOf(0), n.getIntentos());
        assertEquals(now, n.getFechaCreacion());
        assertEquals(now, n.getFechaEnvio());
        assertEquals("test error", n.getError());
    }

    @Test
    @DisplayName("Notificacion @Builder.Default valores por defecto")
    void testNotificacion_BuilderDefault_ValoresPorDefecto() {
        Notificacion n = Notificacion.builder()
                .tipo(TipoNotificacion.CONFIRMACION_PAGO)
                .destinatario("test@email.com")
                .asunto("Asunto")
                .cuerpo("Cuerpo")
                .build();

        assertEquals(EstadoNotificacion.PENDIENTE, n.getEstado());
        assertEquals(Integer.valueOf(0), n.getIntentos());
        assertNotNull(n.getFechaCreacion());
    }

    @Test
    @DisplayName("Notificacion constructor vacío + Setters + Builder Default")
    void testNotificacion_VacioConSettersYBuilderDefault() {
        Notificacion n = Notificacion.builder()
                .id(5L)
                .tipo(TipoNotificacion.RECUPERACION_CLAVE)
                .destinatario("user@test.cl")
                .asunto("Recuperación")
                .cuerpo("cuerpo")
                .build();

        assertEquals(5L, n.getId());
        assertEquals(TipoNotificacion.RECUPERACION_CLAVE, n.getTipo());
        assertEquals(EstadoNotificacion.PENDIENTE, n.getEstado());
        assertEquals(Integer.valueOf(0), n.getIntentos());
        assertNotNull(n.getFechaCreacion());
        assertNull(n.getFechaEnvio());
        assertNull(n.getError());
    }

    @Test
    @DisplayName("NotificacionRequestDTO creación con todos los campos")
    void testRequestDTO_CreacionCompleta() {
        Map<String, Object> vars = Map.of("nombre", "Juan", "monto", "$100");
        NotificacionRequestDTO dto = new NotificacionRequestDTO(
                TipoNotificacion.CONFIRMACION_PAGO,
                "juan@email.com",
                "Asunto personalizado",
                vars
        );

        assertEquals(TipoNotificacion.CONFIRMACION_PAGO, dto.tipo());
        assertEquals("juan@email.com", dto.destinatario());
        assertEquals("Asunto personalizado", dto.asunto());
        assertEquals(vars, dto.variables());
    }

    @Test
    @DisplayName("NotificacionRequestDTO asunto null permitido")
    void testRequestDTO_AsuntoNull() {
        NotificacionRequestDTO dto = new NotificacionRequestDTO(
                TipoNotificacion.ALERTA_SISTEMA,
                "admin@perfulandia.cl",
                null,
                Map.of("error", "timeout")
        );

        assertNull(dto.asunto());
        assertEquals(TipoNotificacion.ALERTA_SISTEMA, dto.tipo());
    }

    @Test
    @DisplayName("NotificacionResponseDTO creación con estado ENVIADO")
    void testResponseDTO_Enviado() {
        LocalDateTime now = LocalDateTime.now();
        NotificacionResponseDTO dto = new NotificacionResponseDTO(
                1L,
                TipoNotificacion.CONFIRMACION_PAGO,
                "cliente@email.com",
                "Confirmación de Pago",
                EstadoNotificacion.ENVIADO,
                1,
                now,
                now,
                null
        );

        assertEquals(1L, dto.id());
        assertEquals(TipoNotificacion.CONFIRMACION_PAGO, dto.tipo());
        assertEquals("cliente@email.com", dto.destinatario());
        assertEquals(EstadoNotificacion.ENVIADO, dto.estado());
        assertEquals(1, dto.intentos());
        assertNotNull(dto.fechaCreacion());
        assertNotNull(dto.fechaEnvio());
        assertNull(dto.error());
    }

    @Test
    @DisplayName("NotificacionResponseDTO creación con estado FALLIDO y error")
    void testResponseDTO_Fallido() {
        LocalDateTime now = LocalDateTime.now();
        NotificacionResponseDTO dto = new NotificacionResponseDTO(
                2L,
                TipoNotificacion.ALERTA_SISTEMA,
                "admin@perfulandia.cl",
                "Alerta del Sistema",
                EstadoNotificacion.FALLIDO,
                3,
                now,
                null,
                "RuntimeException: Error SMTP"
        );

        assertEquals(EstadoNotificacion.FALLIDO, dto.estado());
        assertEquals(3, dto.intentos());
        assertNull(dto.fechaEnvio());
        assertNotNull(dto.error());
    }

    @Test
    @DisplayName("ErrorResponseDTO creación con validationErrors null")
    void testErrorResponseDTO_SinValidationErrors() {
        LocalDateTime now = LocalDateTime.now();
        ErrorResponseDTO dto = new ErrorResponseDTO(
                now, 400, "Bad Request",
                "Error de validación", "/api/test", null
        );

        assertEquals(now, dto.timestamp());
        assertEquals(400, dto.status());
        assertEquals("Bad Request", dto.error());
        assertEquals("Error de validación", dto.message());
        assertEquals("/api/test", dto.path());
        assertNull(dto.validationErrors());
    }

    @Test
    @DisplayName("ErrorResponseDTO creación con validationErrors no null")
    void testErrorResponseDTO_ConValidationErrors() {
        ErrorResponseDTO dto = new ErrorResponseDTO(
                LocalDateTime.now(), 400, "Bad Request",
                "Error en la validación de los datos enviados",
                "/api/notificaciones/enviar",
                Map.of("tipo", "El tipo de notificación es obligatorio",
                       "destinatario", "El destinatario es obligatorio")
        );

        assertNotNull(dto.validationErrors());
        assertFalse(dto.validationErrors().isEmpty());
        assertTrue(dto.validationErrors().containsKey("tipo"));
        assertEquals("El tipo de notificación es obligatorio", dto.validationErrors().get("tipo"));
    }

    @Test
    @DisplayName("ErrorResponseDTO anotado con @JsonInclude(NON_NULL)")
    void testErrorResponseDTO_JsonInclude() throws Exception {
        var annotation = com.fasterxml.jackson.annotation.JsonInclude.class;
        assertNotNull(ErrorResponseDTO.class.getAnnotation(annotation));
    }
}
