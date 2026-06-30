package com.perfulandia.notificaciones.exception;

import com.perfulandia.notificaciones.model.dto.NotificacionRequestDTO;
import jakarta.validation.Valid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas unitarias para GlobalExceptionHandler.
 * Usa un controlador de prueba para provocar cada tipo de excepción
 * y verificar que el handler devuelve la respuesta JSON correcta.
 */
@DisplayName("GlobalExceptionHandler — Pruebas de manejadores de excepción")
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("MethodArgumentNotValidException → 400 + validationErrors")
    void handleValidationException() throws Exception {
        // Given: cuerpo vacío que no pasa validación @Valid del DTO
        String invalidBody = """
                {
                    "destinatario": "",
                    "variables": null
                }
                """;

        // When / Then
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Error en la validación de los datos enviados"))
                .andExpect(jsonPath("$.validationErrors").isMap())
                .andExpect(jsonPath("$.validationErrors.tipo").exists())
                .andExpect(jsonPath("$.validationErrors.variables").exists());
    }

    @Test
    @DisplayName("IllegalArgumentException → 400 + mensaje descriptivo")
    void handleIllegalArgument() throws Exception {
        // Given / When / Then
        mockMvc.perform(get("/test/illegal-argument"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Parámetro inválido: Invalid enumeration value"))
                .andExpect(jsonPath("$.validationErrors").doesNotExist());
    }

    @Test
    @DisplayName("NotificacionNotFoundException → 404 + mensaje con ID")
    void handleNotFound() throws Exception {
        // Given / When / Then
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Notificación no encontrada con ID: 999"))
                .andExpect(jsonPath("$.path").value("/test/not-found"))
                .andExpect(jsonPath("$.validationErrors").doesNotExist());
    }

    @Test
    @DisplayName("Exception genérica → 500 + mensaje genérico")
    void handleGlobalException() throws Exception {
        // Given / When / Then
        mockMvc.perform(get("/test/error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("Ha ocurrido un error inesperado en el servidor"))
                .andExpect(jsonPath("$.validationErrors").doesNotExist());
    }

    @Test
    @DisplayName("EmailSendException → 500 + mensaje descriptivo")
    void handleEmailSendException() throws Exception {
        // Given / When / Then
        mockMvc.perform(get("/test/email-send-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Error al enviar notificación"))
                .andExpect(jsonPath("$.message").value("No se pudo enviar el correo tras 3 intentos"));
    }

    // ─── Controlador de prueba que lanza las excepciones ───

    @RestController
    static class TestController {

        @PostMapping("/test/validation")
        public ResponseEntity<?> triggerValidation(@Valid @RequestBody NotificacionRequestDTO dto) {
            return ResponseEntity.ok().build();
        }

        @GetMapping("/test/illegal-argument")
        public void triggerIllegalArgument() {
            throw new IllegalArgumentException("Invalid enumeration value");
        }

        @GetMapping("/test/not-found")
        public void triggerNotFound() {
            throw new NotificacionNotFoundException(999L);
        }

        @GetMapping("/test/error")
        public void triggerError() {
            throw new RuntimeException("Unexpected server error");
        }

        @GetMapping("/test/email-send-error")
        public void triggerEmailSendError() {
            throw new EmailSendException("No se pudo enviar el correo tras 3 intentos");
        }
    }
}
