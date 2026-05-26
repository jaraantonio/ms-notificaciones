package com.perfulandia.notificaciones.model.dto;

import java.time.LocalDateTime;

import com.perfulandia.notificaciones.model.enums.EstadoNotificacion;
import com.perfulandia.notificaciones.model.enums.TipoNotificacion;

public record NotificacionResponseDTO(
        Integer idNotificacion,
        Integer idUsuario,
        String destinatario,
        String asunto,
        String cuerpoMensaje,
        TipoNotificacion tipo,
        EstadoNotificacion estado,
        LocalDateTime fechaEnvio) {
}