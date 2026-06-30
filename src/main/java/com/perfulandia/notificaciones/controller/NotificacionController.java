package com.perfulandia.notificaciones.controller;

import com.perfulandia.notificaciones.model.dto.NotificacionRequestDTO;
import com.perfulandia.notificaciones.model.dto.NotificacionResponseDTO;
import com.perfulandia.notificaciones.service.NotificacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
@Tag(name = "Notificaciones", description = "Endpoints para envío y consulta de notificaciones por correo electrónico")
public class NotificacionController {

    private final NotificacionService notificacionService;

    @PostMapping("/enviar")
    @Operation(summary = "Enviar correo de notificación",
               description = "Recibe una solicitud de notificación, renderiza la plantilla HTML correspondiente al tipo y envía el correo. Incluye hasta 3 reintentos en caso de fallo.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Correo enviado exitosamente",
                         content = @Content(schema = @Schema(implementation = NotificacionResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "500", description = "Error interno — el correo no pudo enviarse tras 3 reintentos")
    })
    public ResponseEntity<NotificacionResponseDTO> enviarCorreo(
            @Valid @RequestBody NotificacionRequestDTO request) {
        NotificacionResponseDTO response = notificacionService.enviarCorreo(request);
        if (response.estado() == com.perfulandia.notificaciones.model.enums.EstadoNotificacion.FALLIDO) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/logs")
    @Operation(summary = "Consultar historial de notificaciones",
               description = "Devuelve una lista paginada de notificaciones. Permite filtrar opcionalmente por tipo y/o estado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista paginada de notificaciones"),
            @ApiResponse(responseCode = "400", description = "Parámetros de filtro inválidos")
    })
    public ResponseEntity<Page<NotificacionResponseDTO>> obtenerLogs(
            @Parameter(description = "Filtrar por tipo de notificación (ej: CONFIRMACION_PAGO)")
            @RequestParam(required = false) String tipo,
            @Parameter(description = "Filtrar por estado (ej: ENVIADO)")
            @RequestParam(required = false) String estado,
            @Parameter(description = "Paginación y ordenamiento")
            @PageableDefault(size = 20, sort = "fechaCreacion", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<NotificacionResponseDTO> logs = notificacionService.obtenerLogs(pageable, tipo, estado);
        return ResponseEntity.ok(logs);
    }
}
