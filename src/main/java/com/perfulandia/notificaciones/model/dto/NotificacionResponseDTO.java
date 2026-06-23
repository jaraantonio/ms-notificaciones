package com.perfulandia.notificaciones.model.dto;

import com.perfulandia.notificaciones.model.enums.EstadoNotificacion;
import com.perfulandia.notificaciones.model.enums.TipoNotificacion;

import java.time.LocalDateTime;

/**
 * DTO inmutable para la respuesta de una notificación procesada.
 * Refleja el estado final del registro en BD tras el envío (o fallo).
 */
public record NotificacionResponseDTO(
        Long id,
        TipoNotificacion tipo,
        String destinatario,
        String asunto,
        EstadoNotificacion estado,
        int intentos,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaEnvio,
        String error
) {}
