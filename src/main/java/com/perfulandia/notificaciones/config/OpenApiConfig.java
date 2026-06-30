package com.perfulandia.notificaciones.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de Springdoc OpenAPI v3.0.x.
 * Documentación accesible en: /swagger-ui.html
 * Especificación OpenAPI en:   /v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI msNotificacionesOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MS Notificaciones — Perfulandia SPA")
                        .description("""
                                Microservicio de notificaciones para Perfulandia SPA.

                                **Funcionalidades:**
                                - Envío de correos electrónicos con plantillas HTML (Thymeleaf)
                                - Reintentos automáticos (hasta 3) en caso de fallo
                                - Adjuntos PDF para facturas
                                - Consulta paginada de logs con filtros por tipo y estado

                                **Endpoints principales:**
                                - `POST /api/notificaciones/enviar` — Enviar correo
                                - `GET /api/notificaciones/logs` — Consultar historial
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Equipo Perfulandia SPA")
                                .email("soporte@perfulandia.cl"))
                        .license(new License()
                                .name("Uso interno")
                                .url("https://perfulandia.cl")));
    }
}
