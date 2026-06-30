package com.perfulandia.notificaciones.controller;

import com.perfulandia.notificaciones.exception.GlobalExceptionHandler;
import com.perfulandia.notificaciones.model.dto.NotificacionResponseDTO;
import com.perfulandia.notificaciones.model.enums.EstadoNotificacion;
import com.perfulandia.notificaciones.model.enums.TipoNotificacion;
import com.perfulandia.notificaciones.service.NotificacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificacionController — Tests de capa web")
class NotificacionControllerTest {

    @Mock
    private NotificacionService notificacionService;

    @InjectMocks
    private NotificacionController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Nested
    @DisplayName("POST /api/notificaciones/enviar")
    class EnviarCorreo {

        @Test
        @DisplayName("HU-42 · Envío exitoso retorna 201 Created")
        void testEnviarCorreo_Exitoso_201() throws Exception {
            String requestJson = """
                    {
                        "tipo": "CONFIRMACION_PAGO",
                        "destinatario": "cliente@email.com",
                        "asunto": null,
                        "variables": {
                            "nombre": "Juan",
                            "monto": "$45.990",
                            "pedidoId": "#P-001",
                            "fecha": "15/01/2026"
                        },
                        "archivoAdjunto": null
                    }
                    """;

            NotificacionResponseDTO response = new NotificacionResponseDTO(
                    1L, TipoNotificacion.CONFIRMACION_PAGO, "cliente@email.com",
                    "Confirmación de Pago — Perfulandia SPA",
                    EstadoNotificacion.ENVIADO, 1,
                    LocalDateTime.now(), LocalDateTime.now(), null
            );

            when(notificacionService.enviarCorreo(any()))
                    .thenReturn(response);

            mockMvc.perform(post("/api/notificaciones/enviar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.estado").value("ENVIADO"))
                    .andExpect(jsonPath("$.tipo").value("CONFIRMACION_PAGO"))
                    .andExpect(jsonPath("$.intentos").value(1))
                    .andExpect(jsonPath("$.destinatario").value("cliente@email.com"));
        }

        @Test
        @DisplayName("HU-42 · Envío fallido retorna 500 con estado FALLIDO")
        void testEnviarCorreo_Fallido_500() throws Exception {
            String requestJson = """
                    {
                        "tipo": "ALERTA_SISTEMA",
                        "destinatario": "admin@perfulandia.cl",
                        "variables": {
                            "nombreMicroservicio": "ms-pagos",
                            "error": "timeout",
                            "timestamp": "2026-01-01"
                        }
                    }
                    """;

            NotificacionResponseDTO response = new NotificacionResponseDTO(
                    2L, TipoNotificacion.ALERTA_SISTEMA, "admin@perfulandia.cl",
                    "Alerta del Sistema — Perfulandia SPA",
                    EstadoNotificacion.FALLIDO, 3,
                    LocalDateTime.now(), null, "RuntimeException: Error SMTP"
            );

            when(notificacionService.enviarCorreo(any()))
                    .thenReturn(response);

            mockMvc.perform(post("/api/notificaciones/enviar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.estado").value("FALLIDO"))
                    .andExpect(jsonPath("$.intentos").value(3))
                    .andExpect(jsonPath("$.error").isNotEmpty());
        }

        @Test
        @DisplayName("HU-42 · Falta campo obligatorio → 400 + validationErrors")
        void testEnviarCorreo_SinTipo_400() throws Exception {
            String bodySinTipo = """
                    {
                        "destinatario": "test@email.com",
                        "asunto": "Test",
                        "variables": {},
                        "archivoAdjunto": null
                    }
                    """;

            mockMvc.perform(post("/api/notificaciones/enviar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodySinTipo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value("Error en la validación de los datos enviados"))
                    .andExpect(jsonPath("$.validationErrors.tipo").exists());
        }

        @Test
        @DisplayName("HU-42 · Email inválido → 400 + validationErrors.destinatario")
        void testEnviarCorreo_EmailInvalido_400() throws Exception {
            String requestJson = """
                    {
                        "tipo": "CONFIRMACION_PAGO",
                        "destinatario": "no-es-email",
                        "variables": {
                            "nombre": "Test",
                            "monto": "$100",
                            "pedidoId": "#1",
                            "fecha": "2026-01-01"
                        }
                    }
                    """;

            mockMvc.perform(post("/api/notificaciones/enviar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.destinatario").exists());
        }
    }

    @Nested
    @DisplayName("GET /api/notificaciones/logs")
    class ObtenerLogs {

        @Test
        @DisplayName("HU-42 · Sin filtros retorna página paginada")
        void testLogs_SinFiltros_200() throws Exception {
            NotificacionResponseDTO dto = new NotificacionResponseDTO(
                    1L, TipoNotificacion.CONFIRMACION_PAGO, "a@b.cl",
                    "Asunto", EstadoNotificacion.ENVIADO, 1,
                    LocalDateTime.now(), LocalDateTime.now(), null);
            Page<NotificacionResponseDTO> pagina = new PageImpl<>(
                    List.of(dto), PageRequest.of(0, 20), 1
            );

            when(notificacionService.obtenerLogs(any(), isNull(), isNull()))
                    .thenReturn(pagina);

            mockMvc.perform(get("/api/notificaciones/logs")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(1))
                    .andExpect(jsonPath("$.content[0].estado").value("ENVIADO"));
        }

        @Test
        @DisplayName("HU-42 · Filtro por tipo CONFIRMACION_PAGO")
        void testLogs_FiltroTipo_200() throws Exception {
            Page<NotificacionResponseDTO> pagina = new PageImpl<>(
                    List.of(), PageRequest.of(0, 20), 0
            );

            when(notificacionService.obtenerLogs(any(), eq("CONFIRMACION_PAGO"), isNull()))
                    .thenReturn(pagina);

            mockMvc.perform(get("/api/notificaciones/logs")
                            .param("tipo", "CONFIRMACION_PAGO")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        @DisplayName("HU-42 · Filtro por estado FALLIDO")
        void testLogs_FiltroEstado_200() throws Exception {
            Page<NotificacionResponseDTO> pagina = new PageImpl<>(
                    List.of(), PageRequest.of(0, 20), 0
            );

            when(notificacionService.obtenerLogs(any(), isNull(), eq("FALLIDO")))
                    .thenReturn(pagina);

            mockMvc.perform(get("/api/notificaciones/logs")
                            .param("estado", "FALLIDO")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk());
        }
    }
}
