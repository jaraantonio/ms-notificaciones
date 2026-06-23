package com.perfulandia.notificaciones.model.enums;

/**
 * Estados del ciclo de vida de una notificación.
 * PENDIENTE → ENVIADO (éxito) o FALLIDO (tras 3 reintentos).
 */
public enum EstadoNotificacion {
    PENDIENTE,
    ENVIADO,
    FALLIDO
}
