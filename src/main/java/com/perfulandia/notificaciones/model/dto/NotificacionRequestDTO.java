package com.perfulandia.notificaciones.model.dto;

import com.perfulandia.notificaciones.model.enums.TipoNotificacion;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * DTO inmutable para la solicitud de envío de notificación.
 * Si {@code asunto} es null, se genera automáticamente según el tipo.
 * {@code archivoAdjunto} solo se procesa para FACTURA_EMITIDA.
 */
public record NotificacionRequestDTO(

        @NotNull(message = "El tipo de notificación es obligatorio")
        TipoNotificacion tipo,

        @NotBlank(message = "El destinatario es obligatorio")
        @Email(message = "El formato del correo no es válido")
        String destinatario,

        String asunto, // si es null, se genera automático según tipo

        @NotNull(message = "Las variables de contexto son obligatorias")
        Map<String, Object> variables,

        byte[] archivoAdjunto // nullable, solo para FACTURA_EMITIDA

) {}
