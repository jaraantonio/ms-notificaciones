package com.perfulandia.notificaciones.model.dto;

import com.perfulandia.notificaciones.model.enums.TipoNotificacion;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NotificacionRequestDTO — Validación de campos")
class NotificacionRequestDTOTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("DTO válido — sin violaciones")
    void validDto_NoViolations() {
        // Given
        NotificacionRequestDTO dto = new NotificacionRequestDTO(
                TipoNotificacion.CONFIRMACION_PAGO,
                "cliente@email.com",
                "Asunto personalizado",
                Map.of("nombre", "Juan", "monto", "$100")
        );

        // When
        Set<ConstraintViolation<NotificacionRequestDTO>> violations = validator.validate(dto);

        // Then
        assertTrue(violations.isEmpty(), "DTO válido no debe tener violaciones");
    }

    @Test
    @DisplayName("DTO con asunto null — sigue siendo válido (auto-generado)")
    void validDto_AsuntoNull_NoViolations() {
        // Given
        NotificacionRequestDTO dto = new NotificacionRequestDTO(
                TipoNotificacion.ALERTA_SISTEMA,
                "admin@perfulandia.cl",
                null,
                Map.of("error", "timeout")
        );

        // When
        Set<ConstraintViolation<NotificacionRequestDTO>> violations = validator.validate(dto);

        // Then
        assertTrue(violations.isEmpty(), "Asunto null está permitido");
    }

    @Test
    @DisplayName("tipo null — violación @NotNull")
    void tipoNull_HasViolation() {
        // Given
        NotificacionRequestDTO dto = new NotificacionRequestDTO(
                null,
                "cliente@email.com",
                null,
                Map.of("nombre", "Test")
        );

        // When
        Set<ConstraintViolation<NotificacionRequestDTO>> violations = validator.validate(dto);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("tipo")));
    }

    @Test
    @DisplayName("destinatario blank — violación @NotBlank")
    void destinatarioBlank_HasViolation() {
        // Given
        NotificacionRequestDTO dto = new NotificacionRequestDTO(
                TipoNotificacion.CONFIRMACION_PAGO,
                "",
                null,
                Map.of("nombre", "Test")
        );

        // When
        Set<ConstraintViolation<NotificacionRequestDTO>> violations = validator.validate(dto);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("destinatario")));
    }

    @Test
    @DisplayName("destinatario email inválido — violación @Email")
    void destinatarioEmailInvalido_HasViolation() {
        // Given
        NotificacionRequestDTO dto = new NotificacionRequestDTO(
                TipoNotificacion.CONFIRMACION_PAGO,
                "no-es-un-email-valido",
                null,
                Map.of("nombre", "Test")
        );

        // When
        Set<ConstraintViolation<NotificacionRequestDTO>> violations = validator.validate(dto);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("destinatario")));
    }

    @Test
    @DisplayName("variables null — violación @NotNull")
    void variablesNull_HasViolation() {
        // Given
        NotificacionRequestDTO dto = new NotificacionRequestDTO(
                TipoNotificacion.CONFIRMACION_PAGO,
                "cliente@email.com",
                null,
                null
        );

        // When
        Set<ConstraintViolation<NotificacionRequestDTO>> violations = validator.validate(dto);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("variables")));
    }

    @Test
    @DisplayName("Múltiples campos inválidos — todas las violaciones presentes")
    void multipleInvalidFields_AllViolationsPresent() {
        // Given
        NotificacionRequestDTO dto = new NotificacionRequestDTO(
                null,
                "",
                null,
                null
        );

        // When
        Set<ConstraintViolation<NotificacionRequestDTO>> violations = validator.validate(dto);

        // Then
        assertEquals(3, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("tipo")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("destinatario")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("variables")));
    }
}
