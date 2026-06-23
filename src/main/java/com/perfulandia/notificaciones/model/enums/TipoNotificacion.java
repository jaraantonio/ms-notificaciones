package com.perfulandia.notificaciones.model.enums;

/**
 * Tipos de notificación que puede enviar el sistema.
 * Cada tipo determina la plantilla Thymeleaf a utilizar
 * y las variables de contexto esperadas.
 */
public enum TipoNotificacion {
    CONFIRMACION_PAGO,
    ACTUALIZACION_ENVIO,
    FACTURA_EMITIDA,
    RECUPERACION_CLAVE,
    ALERTA_SISTEMA
}
