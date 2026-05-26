package com.perfulandia.notificaciones.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.perfulandia.notificaciones.model.dto.NotificacionRequestDTO;
import com.perfulandia.notificaciones.model.dto.NotificacionResponseDTO;
import com.perfulandia.notificaciones.service.NotificacionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService notificacionService;

    @PostMapping
    public ResponseEntity<NotificacionResponseDTO> enviarNotificacion(
            @Valid @RequestBody NotificacionRequestDTO request) {
        NotificacionResponseDTO response = notificacionService.procesarNotificacion(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/health-check-usuarios")
    public ResponseEntity<Map<String, String>> verificarSaludUsuarios() {
        boolean isUp = notificacionService.monitorearAlertasDelSistema();
        if (isUp) {
            return ResponseEntity.ok(Map.of("status", "ms-usuarios funcionando correctamente"));
        } else {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("status", "ms-usuarios caído. Alerta enviada al administrador."));
        }
    }
}