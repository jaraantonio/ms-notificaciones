package com.perfulandia.notificaciones.exception;

/**
 * Excepción lanzada cuando no se encuentra una notificación por su ID.
 */
public class NotificacionNotFoundException extends RuntimeException {

    public NotificacionNotFoundException(Long id) {
        super("Notificación no encontrada con ID: " + id);
    }
}
