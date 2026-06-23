package com.perfulandia.notificaciones.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO estándar para respuestas de error en la API.
 * {@code validationErrors} solo se incluye si hay errores de validación.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponseDTO(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> validationErrors
) {}
