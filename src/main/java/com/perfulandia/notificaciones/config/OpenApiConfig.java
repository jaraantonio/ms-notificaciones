package com.perfulandia.notificaciones.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de Springdoc OpenAPI v3.0.x.
 * Documentación accesible en: /swagger-ui.html
 * Especificación OpenAPI en:   /v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
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
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
