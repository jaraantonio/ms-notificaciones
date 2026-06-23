package com.perfulandia.notificaciones.exception;

/**
 * Excepción lanzada cuando el envío de un correo falla
 * tras agotar los 3 reintentos.
 */
public class EmailSendException extends RuntimeException {

    public EmailSendException(String message) {
        super(message);
    }

    public EmailSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
