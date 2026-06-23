package com.perfulandia.notificaciones;

import com.perfulandia.notificaciones.model.enums.EstadoNotificacion;
import com.perfulandia.notificaciones.model.enums.TipoNotificacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("NotificacionesApplication — Context load + enums + exceptions")
class NotificacionesApplicationTests {

    @Test
    @DisplayName("Contexto de Spring carga correctamente")
    void contextLoads() {
        // Si llega aquí, el contexto cargó sin errores
    }

    @Test
    @DisplayName("TipoNotificacion — todos los valores definidos")
    void tipoNotificacion_Valores() {
        TipoNotificacion[] tipos = TipoNotificacion.values();
        assertEquals(5, tipos.length);
        assertNotNull(TipoNotificacion.valueOf("CONFIRMACION_PAGO"));
        assertNotNull(TipoNotificacion.valueOf("ACTUALIZACION_ENVIO"));
        assertNotNull(TipoNotificacion.valueOf("FACTURA_EMITIDA"));
        assertNotNull(TipoNotificacion.valueOf("RECUPERACION_CLAVE"));
        assertNotNull(TipoNotificacion.valueOf("ALERTA_SISTEMA"));
    }

    @Test
    @DisplayName("EstadoNotificacion — todos los valores definidos")
    void estadoNotificacion_Valores() {
        EstadoNotificacion[] estados = EstadoNotificacion.values();
        assertEquals(3, estados.length);
        assertNotNull(EstadoNotificacion.valueOf("PENDIENTE"));
        assertNotNull(EstadoNotificacion.valueOf("ENVIADO"));
        assertNotNull(EstadoNotificacion.valueOf("FALLIDO"));
    }

    @Test
    @DisplayName("EmailSendException — constructores")
    void emailSendException_Constructores() {
        var ex1 = new com.perfulandia.notificaciones.exception.EmailSendException("Test error");
        assertEquals("Test error", ex1.getMessage());

        var cause = new RuntimeException("Causa");
        var ex2 = new com.perfulandia.notificaciones.exception.EmailSendException("Error con causa", cause);
        assertEquals("Error con causa", ex2.getMessage());
        assertEquals(cause, ex2.getCause());
    }

    @Test
    @DisplayName("NotificacionNotFoundException — constructor")
    void notificacionNotFoundException_Constructor() {
        var ex = new com.perfulandia.notificaciones.exception.NotificacionNotFoundException(99L);
        assertTrue(ex.getMessage().contains("99"));
    }
}
