package com.perfulandia.notificaciones.model.dto;

import com.perfulandia.notificaciones.model.enums.TipoNotificacion;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NotificacionRequestDTO(
        @NotNull(message = "El id del usuario es obligatorio") Integer idUsuario,

        @NotBlank(message = "El destinatario es obligatorio") @Email(message = "El formato del correo destinatario no es válido") @Size(max = 150, message = "El destinatario no puede exceder los 150 caracteres") String destinatario,

        @NotBlank(message = "El asunto es obligatorio") @Size(max = 200, message = "El asunto no puede exceder los 200 caracteres") String asunto,

        @NotBlank(message = "El cuerpo del mensaje es obligatorio") String cuerpoMensaje,

        @NotNull(message = "El tipo de notificación es obligatorio") TipoNotificacion tipo) {
}