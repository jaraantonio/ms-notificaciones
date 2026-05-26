package com.perfulandia.notificaciones.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "ms-usuarios", url = "http://localhost:8081")
public interface UsuarioClient {

    @GetMapping("/actuator/health")
    String checkHealth();
}