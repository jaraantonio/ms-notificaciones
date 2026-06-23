package com.perfulandia.notificaciones;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.perfulandia.notificaciones.controller.NotificacionController;
import com.perfulandia.notificaciones.exception.GlobalExceptionHandler;
import com.perfulandia.notificaciones.model.dto.NotificacionRequestDTO;
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
import java.util.Map;

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
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    // POST /api/notificaciones/enviar
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/notificaciones/enviar")
    class EnviarCorreo {

        @Test
        @DisplayName("HU-01 — Envío exitoso retorna 201 + JSON completo")
        void enviarCorreo_Exitoso_201() throws Exception {
            NotificacionRequestDTO request = new NotificacionRequestDTO(
                    TipoNotificacion.CONFIRMACION_PAGO,
                    "cliente@email.com",
                    null,
                    Map.of("nombre", "Juan", "monto", "$45.990",
                           "pedidoId", "#P-001", "fecha", "15/01/2026"),
                    null
            );
            NotificacionResponseDTO response = new NotificacionResponseDTO(
                    1L, TipoNotificacion.CONFIRMACION_PAGO, "cliente@email.com",
                    "Confirmación de Pago — Perfulandia SPA",
                    EstadoNotificacion.ENVIADO, 1,
                    LocalDateTime.now(), LocalDateTime.now(), null
            );

            when(notificacionService.enviarCorreo(any(NotificacionRequestDTO.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/api/notificaciones/enviar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.estado").value("ENVIADO"))
                    .andExpect(jsonPath("$.tipo").value("CONFIRMACION_PAGO"))
                    .andExpect(jsonPath("$.intentos").value(1))
                    .andExpect(jsonPath("$.destinatario").value("cliente@email.com"));
        }

        @Test
        @DisplayName("HU-05 — Envío fallido retorna 500 con estado FALLIDO")
        void enviarCorreo_Fallido_500() throws Exception {
            NotificacionRequestDTO request = new NotificacionRequestDTO(
                    TipoNotificacion.ALERTA_SISTEMA,
                    "admin@perfulandia.cl",
                    null,
                    Map.of("nombreMicroservicio", "ms-pagos", "error", "timeout",
                           "timestamp", "2026-01-01"),
                    null
            );
            NotificacionResponseDTO response = new NotificacionResponseDTO(
                    2L, TipoNotificacion.ALERTA_SISTEMA, "admin@perfulandia.cl",
                    "Alerta del Sistema — Perfulandia SPA",
                    EstadoNotificacion.FALLIDO, 3,
                    LocalDateTime.now(), null, "RuntimeException: Error SMTP"
            );

            when(notificacionService.enviarCorreo(any(NotificacionRequestDTO.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/api/notificaciones/enviar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.estado").value("FALLIDO"))
                    .andExpect(jsonPath("$.intentos").value(3))
                    .andExpect(jsonPath("$.error").isNotEmpty());
        }

        @Test
        @DisplayName("HU-06 — Falta campo tipo → 400 + validationErrors")
        void enviarCorreo_SinTipo_400() throws Exception {
            String bodySinTipo = """
                    {
                      "destinatario": "test@email.com",
                      "asunto": "Test",
                      "variables": {},
                      "archivoAdjunto": null
                    }""";

            mockMvc.perform(post("/api/notificaciones/enviar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodySinTipo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("Error en la validación de los datos enviados"))
                    .andExpect(jsonPath("$.validationErrors.tipo").exists());
        }

        @Test
        @DisplayName("HU-07 — Email inválido → 400 + validationErrors.destinatario")
        void enviarCorreo_EmailInvalido_400() throws Exception {
            NotificacionRequestDTO request = new NotificacionRequestDTO(
                    TipoNotificacion.CONFIRMACION_PAGO,
                    "no-es-email",
                    null,
                    Map.of("nombre", "Test", "monto", "$100",
                           "pedidoId", "#1", "fecha", "2026-01-01"),
                    null
            );

            mockMvc.perform(post("/api/notificaciones/enviar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.destinatario").exists());
        }

        @Test
        @DisplayName("HU-06b — Destinatario vacío → 400")
        void enviarCorreo_DestinatarioVacio_400() throws Exception {
            NotificacionRequestDTO request = new NotificacionRequestDTO(
                    TipoNotificacion.RECUPERACION_CLAVE,
                    "",
                    null,
                    Map.of("nombre", "Test", "enlaceRestablecimiento", "https://x.com"),
                    null
            );

            mockMvc.perform(post("/api/notificaciones/enviar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.destinatario").exists());
        }

        @Test
        @DisplayName("HU-04 — RECUPERACION_CLAVE exitoso")
        void enviarCorreo_RecuperacionClave_201() throws Exception {
            NotificacionRequestDTO request = new NotificacionRequestDTO(
                    TipoNotificacion.RECUPERACION_CLAVE,
                    "pedro@email.com",
                    null,
                    Map.of("nombre", "Pedro",
                           "enlaceRestablecimiento", "https://perfulandia.cl/restablecer?token=abc"),
                    null
            );
            NotificacionResponseDTO response = new NotificacionResponseDTO(
                    4L, TipoNotificacion.RECUPERACION_CLAVE, "pedro@email.com",
                    "Recuperación de Clave — Perfulandia SPA",
                    EstadoNotificacion.ENVIADO, 1,
                    LocalDateTime.now(), LocalDateTime.now(), null
            );

            when(notificacionService.enviarCorreo(any(NotificacionRequestDTO.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/api/notificaciones/enviar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.tipo").value("RECUPERACION_CLAVE"));
        }

        @Test
        @DisplayName("HU-02 — ACTUALIZACION_ENVIO con variables completas")
        void enviarCorreo_ActualizacionEnvio_201() throws Exception {
            NotificacionRequestDTO request = new NotificacionRequestDTO(
                    TipoNotificacion.ACTUALIZACION_ENVIO,
                    "juan@email.com",
                    "Actualización de pedido #E-101",
                    Map.of("nombre", "Juan", "pedidoId", "#E-101",
                           "estadoEnvio", "EN CAMINO",
                           "linkSeguimiento", "https://perfulandia.cl/seguimiento/E-101"),
                    null
            );
            NotificacionResponseDTO response = new NotificacionResponseDTO(
                    5L, TipoNotificacion.ACTUALIZACION_ENVIO, "juan@email.com",
                    "Actualización de pedido #E-101",
                    EstadoNotificacion.ENVIADO, 1,
                    LocalDateTime.now(), LocalDateTime.now(), null
            );

            when(notificacionService.enviarCorreo(any(NotificacionRequestDTO.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/api/notificaciones/enviar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.asunto").value("Actualización de pedido #E-101"));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // GET /api/notificaciones/logs
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/notificaciones/logs")
    class ObtenerLogs {

        @Test
        @DisplayName("HU-08 — Sin filtros retorna página paginada")
        void logs_SinFiltros_200() throws Exception {
            NotificacionResponseDTO dto = new NotificacionResponseDTO(
                    1L, TipoNotificacion.CONFIRMACION_PAGO, "a@b.cl",
                    "Asunto", EstadoNotificacion.ENVIADO, 1,
                    LocalDateTime.now(), LocalDateTime.now(), null);
            Page<NotificacionResponseDTO> pagina = new PageImpl<>(
                    List.of(dto), PageRequest.of(0, 20), 1
            );

            when(notificacionService.obtenerLogs(any(), isNull(), isNull()))
                    .thenReturn(pagina);

            mockMvc.perform(get("/api/notificaciones/logs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(1))
                    .andExpect(jsonPath("$.content[0].estado").value("ENVIADO"));
        }

        @Test
        @DisplayName("HU-09 — Filtro por tipo CONFIRMACION_PAGO")
        void logs_FiltroTipo_200() throws Exception {
            Page<NotificacionResponseDTO> pagina = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

            when(notificacionService.obtenerLogs(any(), eq("CONFIRMACION_PAGO"), isNull()))
                    .thenReturn(pagina);

            mockMvc.perform(get("/api/notificaciones/logs")
                            .param("tipo", "CONFIRMACION_PAGO"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("HU-10 — Filtro por estado FALLIDO")
        void logs_FiltroEstado_200() throws Exception {
            Page<NotificacionResponseDTO> pagina = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

            when(notificacionService.obtenerLogs(any(), isNull(), eq("FALLIDO")))
                    .thenReturn(pagina);

            mockMvc.perform(get("/api/notificaciones/logs")
                            .param("estado", "FALLIDO"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("HU-12 — Filtro con tipo inválido → 400 (IllegalArgumentException)")
        void logs_TipoInvalido_400() throws Exception {
            when(notificacionService.obtenerLogs(any(), eq("INVALIDO"), isNull()))
                    .thenThrow(new IllegalArgumentException(
                            "No enum constant com.perfulandia.notificaciones.model.enums.TipoNotificacion.INVALIDO"));

            mockMvc.perform(get("/api/notificaciones/logs")
                            .param("tipo", "INVALIDO"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(
                            "Parámetro inválido: No enum constant com.perfulandia.notificaciones.model.enums.TipoNotificacion.INVALIDO"));
        }

        @Test
        @DisplayName("HU-11 — Filtro por tipo + estado combinados")
        void logs_FiltroTipoYEstado_200() throws Exception {
            NotificacionResponseDTO dto = new NotificacionResponseDTO(
                    9L, TipoNotificacion.ALERTA_SISTEMA, "admin@perfulandia.cl",
                    "Alerta", EstadoNotificacion.ENVIADO, 1,
                    LocalDateTime.now(), LocalDateTime.now(), null);
            Page<NotificacionResponseDTO> pagina = new PageImpl<>(
                    List.of(dto), PageRequest.of(0, 20), 1
            );

            when(notificacionService.obtenerLogs(any(), eq("ALERTA_SISTEMA"), eq("ENVIADO")))
                    .thenReturn(pagina);

            mockMvc.perform(get("/api/notificaciones/logs")
                            .param("tipo", "ALERTA_SISTEMA")
                            .param("estado", "ENVIADO"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].tipo").value("ALERTA_SISTEMA"));
        }
    }
}
