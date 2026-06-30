package com.perfulandia.notificaciones.service;

import com.perfulandia.notificaciones.model.dto.NotificacionRequestDTO;
import com.perfulandia.notificaciones.model.dto.NotificacionResponseDTO;
import com.perfulandia.notificaciones.model.entity.Notificacion;
import com.perfulandia.notificaciones.model.enums.EstadoNotificacion;
import com.perfulandia.notificaciones.model.enums.TipoNotificacion;
import com.perfulandia.notificaciones.repository.NotificacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificacionService — Pruebas unitarias")
class NotificacionServiceTest {

    @Mock
    private NotificacionRepository repository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private NotificacionService notificacionService;

    private NotificacionRequestDTO requestValida;

    @BeforeEach
    void setUp() {
        requestValida = new NotificacionRequestDTO(
                TipoNotificacion.CONFIRMACION_PAGO,
                "cliente@email.com",
                null,
                Map.of("nombre", "Juan", "monto", "$45.990", "pedidoId", "#P-001", "fecha", "15/01/2026")
        );
    }

    @Test
    @DisplayName("Envío exitoso al primer intento")
    void testEnviarCorreo_Exitoso_PrimerIntento() throws Exception {
        when(emailService.renderizarHtml(eq(TipoNotificacion.CONFIRMACION_PAGO), anyMap()))
                .thenReturn("<html>Confirmación de pago</html>");
        when(repository.save(any(Notificacion.class))).thenAnswer(inv -> {
            Notificacion n = inv.getArgument(0);
            if (n.getId() == null) n.setId(1L);
            return n;
        });
        doNothing().when(emailService).enviarCorreoHtml(any(Notificacion.class));

        NotificacionResponseDTO response = notificacionService.enviarCorreo(requestValida);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals(EstadoNotificacion.ENVIADO, response.estado());
        assertEquals("Confirmación de Pago — Perfulandia SPA", response.asunto());
        assertEquals(1, response.intentos());
        assertNotNull(response.fechaEnvio());
        assertNull(response.error());

        verify(emailService, times(1)).enviarCorreoHtml(any(Notificacion.class));
        verify(repository, atLeast(2)).save(any(Notificacion.class));
    }

    @Test
    @DisplayName("3 reintentos fallidos lanza EmailSendException y estado final FALLIDO")
    void testEnviarCorreo_TresReintentosFallidos_LanzaEmailSendException() throws Exception {
        when(emailService.renderizarHtml(any(), anyMap())).thenReturn("<html>...</html>");
        when(repository.save(any(Notificacion.class))).thenAnswer(inv -> {
            Notificacion n = inv.getArgument(0);
            if (n.getId() == null) n.setId(2L);
            return n;
        });
        doThrow(new RuntimeException("Error SMTP"))
                .when(emailService).enviarCorreoHtml(any(Notificacion.class));

        // La excepción se lanza después de 3 reintentos fallidos
        assertThrows(com.perfulandia.notificaciones.exception.EmailSendException.class,
                () -> notificacionService.enviarCorreo(requestValida));

        verify(emailService, times(3)).enviarCorreoHtml(any(Notificacion.class));
        verify(repository, atLeast(4)).save(any(Notificacion.class));
    }

    @Test
    @DisplayName("Éxito en el segundo intento")
    void testEnviarCorreo_Exito_SegundoIntento() throws Exception {
        when(emailService.renderizarHtml(any(), anyMap())).thenReturn("<html>...</html>");
        when(repository.save(any(Notificacion.class))).thenAnswer(inv -> {
            Notificacion n = inv.getArgument(0);
            if (n.getId() == null) n.setId(3L);
            return n;
        });
        doThrow(new RuntimeException("Fallo temporal"))
                .doNothing()
                .when(emailService).enviarCorreoHtml(any(Notificacion.class));

        NotificacionResponseDTO response = notificacionService.enviarCorreo(requestValida);

        assertEquals(EstadoNotificacion.ENVIADO, response.estado());
        assertEquals(2, response.intentos());
        assertNotNull(response.fechaEnvio());

        verify(emailService, times(2)).enviarCorreoHtml(any(Notificacion.class));
    }

    @Test
    @DisplayName("FACTURA_EMITIDA con PDF adjunto")
    void testEnviarCorreo_FacturaEmitida_ConAdjunto() throws Exception {
        NotificacionRequestDTO facturaRequest = new NotificacionRequestDTO(
                TipoNotificacion.FACTURA_EMITIDA,
                "empresa@cliente.cl",
                "Factura #F-001",
                Map.of("nombre", "Empresa Ltda", "monto", "$890.000", "pedidoId", "#F-001", "fecha", "20/01/2026")
        );

        when(emailService.renderizarHtml(eq(TipoNotificacion.FACTURA_EMITIDA), anyMap()))
                .thenReturn("<html>Factura</html>");
        when(repository.save(any(Notificacion.class))).thenAnswer(inv -> {
            Notificacion n = inv.getArgument(0);
            if (n.getId() == null) n.setId(4L);
            return n;
        });
        doNothing().when(emailService).enviarCorreoHtml(any(Notificacion.class));

        NotificacionResponseDTO response = notificacionService.enviarCorreo(facturaRequest);

        assertEquals(TipoNotificacion.FACTURA_EMITIDA, response.tipo());
        assertEquals(EstadoNotificacion.ENVIADO, response.estado());
        assertEquals("Factura #F-001", response.asunto());
        verify(emailService, times(1)).enviarCorreoHtml(any(Notificacion.class));
    }

    @Test
    @DisplayName("Asunto null se auto-genera según el tipo")
    void testEnviarCorreo_AsuntoNull_AutoGenerado() throws Exception {
        when(emailService.renderizarHtml(any(), anyMap())).thenReturn("<html>...</html>");
        when(repository.save(any(Notificacion.class))).thenAnswer(inv -> {
            Notificacion n = inv.getArgument(0);
            if (n.getId() == null) n.setId(5L);
            return n;
        });
        doNothing().when(emailService).enviarCorreoHtml(any(Notificacion.class));

        NotificacionResponseDTO response = notificacionService.enviarCorreo(requestValida);

        assertEquals("Confirmación de Pago — Perfulandia SPA", response.asunto());
    }

    @Test
    @DisplayName("obtenerLogs sin filtros usa findAll paginado")
    void testObtenerLogs_SinFiltros_Paginado() {
        Pageable pageable = PageRequest.of(0, 10);
        List<Notificacion> notificaciones = List.of(
                Notificacion.builder().id(1L).tipo(TipoNotificacion.CONFIRMACION_PAGO)
                        .destinatario("a@b.cl").asunto("Test")
                        .cuerpo("...").estado(EstadoNotificacion.ENVIADO)
                        .intentos(1).fechaCreacion(LocalDateTime.now()).build()
        );
        Page<Notificacion> pagina = new PageImpl<>(notificaciones, pageable, 1);
        when(repository.findAll(pageable)).thenReturn(pagina);

        Page<NotificacionResponseDTO> result = notificacionService.obtenerLogs(pageable, null, null);

        assertEquals(1, result.getTotalElements());
        assertEquals(EstadoNotificacion.ENVIADO, result.getContent().get(0).estado());
        verify(repository, times(1)).findAll(pageable);
    }

    @Test
    @DisplayName("obtenerLogs filtrado por tipo")
    void testObtenerLogs_FiltroTipo() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notificacion> paginaVacia = Page.empty();
        when(repository.findByTipo(eq(TipoNotificacion.ALERTA_SISTEMA), any(Pageable.class)))
                .thenReturn(paginaVacia);

        Page<NotificacionResponseDTO> result = notificacionService.obtenerLogs(pageable, "ALERTA_SISTEMA", null);

        assertEquals(0, result.getTotalElements());
        verify(repository, times(1)).findByTipo(eq(TipoNotificacion.ALERTA_SISTEMA), any(Pageable.class));
    }

    @Test
    @DisplayName("obtenerLogs filtrado por estado")
    void testObtenerLogs_FiltroEstado() {
        Pageable pageable = PageRequest.of(0, 10);
        List<Notificacion> fallidas = List.of(
                Notificacion.builder().id(6L).tipo(TipoNotificacion.FACTURA_EMITIDA)
                        .destinatario("x@y.cl").asunto("Fallo").cuerpo("...")
                        .estado(EstadoNotificacion.FALLIDO).intentos(3)
                        .fechaCreacion(LocalDateTime.now())
                        .error("Error SMTP").build()
        );
        Page<Notificacion> paginaFallidas = new PageImpl<>(fallidas, pageable, 1);
        when(repository.findByEstado(eq(EstadoNotificacion.FALLIDO), any(Pageable.class)))
                .thenReturn(paginaFallidas);

        Page<NotificacionResponseDTO> result = notificacionService.obtenerLogs(pageable, null, "FALLIDO");

        assertEquals(1, result.getTotalElements());
        assertEquals(EstadoNotificacion.FALLIDO, result.getContent().get(0).estado());
        assertEquals(3, result.getContent().get(0).intentos());
    }

    @Test
    @DisplayName("obtenerLogs filtrado por tipo y estado combinados")
    void testObtenerLogs_FiltroTipoYEstado() {
        Pageable pageable = PageRequest.of(0, 10);
        List<Notificacion> resultados = List.of(
                Notificacion.builder().id(7L).tipo(TipoNotificacion.ALERTA_SISTEMA)
                        .destinatario("admin@perfulandia.cl").asunto("Alerta").cuerpo("...")
                        .estado(EstadoNotificacion.ENVIADO).intentos(1)
                        .fechaCreacion(LocalDateTime.now()).build()
        );
        Page<Notificacion> pagina = new PageImpl<>(resultados, pageable, 1);
        when(repository.findByTipoAndEstado(
                eq(TipoNotificacion.ALERTA_SISTEMA), eq(EstadoNotificacion.ENVIADO), any(Pageable.class)))
                .thenReturn(pagina);

        Page<NotificacionResponseDTO> result = notificacionService.obtenerLogs(pageable, "ALERTA_SISTEMA", "ENVIADO");

        assertEquals(1, result.getTotalElements());
        assertEquals(TipoNotificacion.ALERTA_SISTEMA, result.getContent().get(0).tipo());
        assertEquals(EstadoNotificacion.ENVIADO, result.getContent().get(0).estado());
        verify(repository, times(1))
                .findByTipoAndEstado(eq(TipoNotificacion.ALERTA_SISTEMA), eq(EstadoNotificacion.ENVIADO), any(Pageable.class));
    }

    @Test
    @DisplayName("obtenerLogs tipo inválido lanza IllegalArgumentException")
    void testObtenerLogs_TipoInvalido_LanzaIllegalArgument() {
        Pageable pageable = PageRequest.of(0, 10);

        assertThrows(IllegalArgumentException.class,
                () -> notificacionService.obtenerLogs(pageable, "TIPO_INEXISTENTE", null));

        verify(repository, never()).findAll(any(Pageable.class));
        verify(repository, never()).findByTipo(any(), any(Pageable.class));
        verify(repository, never()).findByEstado(any(), any(Pageable.class));
        verify(repository, never()).findByTipoAndEstado(any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("mapearAResponse intentos null se mapea a 0")
    void testMapearAResponse_IntentosNull_MapeaACero() {
        Pageable pageable = PageRequest.of(0, 10);
        Notificacion notificacion = Notificacion.builder()
                .id(10L)
                .tipo(TipoNotificacion.ALERTA_SISTEMA)
                .destinatario("admin@perfulandia.cl")
                .asunto("Alerta")
                .cuerpo("...")
                .estado(EstadoNotificacion.ENVIADO)
                .intentos(null)
                .fechaCreacion(LocalDateTime.now())
                .build();
        Page<Notificacion> pagina = new PageImpl<>(List.of(notificacion), pageable, 1);
        when(repository.findAll(pageable)).thenReturn(pagina);

        Page<NotificacionResponseDTO> result = notificacionService.obtenerLogs(pageable, null, null);

        assertEquals(1, result.getTotalElements());
        assertEquals(0, result.getContent().get(0).intentos());
    }

    @Test
    @DisplayName("mapearAResponse intentos con valor se mapea correctamente")
    void testMapearAResponse_IntentosConValor() {
        Pageable pageable = PageRequest.of(0, 10);
        Notificacion notificacion = Notificacion.builder()
                .id(11L)
                .tipo(TipoNotificacion.CONFIRMACION_PAGO)
                .destinatario("cliente@email.com")
                .asunto("Asunto")
                .cuerpo("...")
                .estado(EstadoNotificacion.FALLIDO)
                .intentos(3)
                .fechaCreacion(LocalDateTime.now())
                .error("Error SMTP tras 3 reintentos")
                .build();
        Page<Notificacion> pagina = new PageImpl<>(List.of(notificacion), pageable, 1);
        when(repository.findByEstado(eq(EstadoNotificacion.FALLIDO), any(Pageable.class)))
                .thenReturn(pagina);

        Page<NotificacionResponseDTO> result = notificacionService.obtenerLogs(pageable, null, "FALLIDO");

        assertEquals(1, result.getTotalElements());
        assertEquals(3, result.getContent().get(0).intentos());
        assertEquals("Error SMTP tras 3 reintentos", result.getContent().get(0).error());
    }

    @Test
    @DisplayName("obtenerLogs filtros combinados sin resultados retorna página vacía")
    void testObtenerLogs_Filtros_SinResultados() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notificacion> paginaVacia = Page.empty();
        when(repository.findByTipoAndEstado(
                eq(TipoNotificacion.FACTURA_EMITIDA), eq(EstadoNotificacion.PENDIENTE), any(Pageable.class)))
                .thenReturn(paginaVacia);

        Page<NotificacionResponseDTO> result = notificacionService.obtenerLogs(pageable, "FACTURA_EMITIDA", "PENDIENTE");

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(repository, times(1))
                .findByTipoAndEstado(eq(TipoNotificacion.FACTURA_EMITIDA), eq(EstadoNotificacion.PENDIENTE), any(Pageable.class));
    }
}
