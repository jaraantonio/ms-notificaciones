# Microservicio Notificaciones

## Descripción

Microservicio de envío de correos electrónicos con plantillas HTML (Thymeleaf), reintentos automáticos y registro de histórico para Perfulandia SPA. Recibe solicitudes de otros microservicios (Usuarios, Pagos, Envíos) y despacha notificaciones transaccionales.

- Historias de usuario: HU-42, HU-43.
- Swagger/OpenAPI disponible en: http://localhost:8089/swagger-ui.html

## Estudiante

Antonio Jara

## Tecnologías

- Java 25, Spring Boot 4.1.0, Spring Web MVC, Spring Mail, JPA/Hibernate
- Thymeleaf (plantillas HTML para correos), Jakarta Validation
- MariaDB 11.8 (compatible con MySQL 8.x para Duoc/XAMPP)
- Maven, Flyway (scripts en `db/migration/V1__init.sql` — desactivado temporalmente porque MariaDB 11.8 no es compatible con Flyway 12.x; en Duoc con MySQL 8.x funciona activando `enabled: true` + `ddl-auto: validate`), Swagger/OpenAPI, JaCoCo

## Registros de prueba (poblado de la BD con data.sql)

Al iniciar la aplicación se insertan automáticamente 10 registros, cubriendo todos los tipos y estados.

| ID | Tipo                | Estado    | Intentos | Descripción                              |
|----|---------------------|-----------|----------|------------------------------------------|
| 1  | CONFIRMACION_PAGO   | ENVIADO   | 1        | Pago confirmado pedido #P-2026-001       |
| 2  | CONFIRMACION_PAGO   | PENDIENTE | 0        | Pago pendiente de envío                  |
| 3  | ACTUALIZACION_ENVIO | ENVIADO   | 1        | Envío en camino #E-2026-101              |
| 4  | ACTUALIZACION_ENVIO | ENVIADO   | 1        | Envío actualizado #E-2026-102            |
| 5  | FACTURA_EMITIDA     | ENVIADO   | 1        | Factura #F-2026-050 con PDF adjunto      |
| 6  | FACTURA_EMITIDA     | FALLIDO   | 3        | Factura #F-2026-051 — 3 reintentos fallidos |
| 7  | RECUPERACION_CLAVE  | ENVIADO   | 1        | Restablecimiento de contraseña           |
| 8  | RECUPERACION_CLAVE  | PENDIENTE | 0        | Solicitud pendiente de envío             |
| 9  | ALERTA_SISTEMA      | ENVIADO   | 1        | Alerta crítica ms-pagos                  |
| 10 | ALERTA_SISTEMA      | FALLIDO   | 3        | Alerta ms-envios — 3 reintentos fallidos |

## Endpoints

### Envío (público — interno entre MS)

| Método | Ruta                           | HU     | Descripción                                  |
|--------|--------------------------------|--------|----------------------------------------------|
| POST   | `/api/notificaciones/enviar`   | HU-42 | Enviar correo con plantilla HTML (reintentos automáticos) |

### Consulta (admin)

| Método | Ruta                           | HU          | Descripción                                  |
|--------|--------------------------------|-------------|----------------------------------------------|
| GET    | `/api/notificaciones/logs`     | HU-42 | Historial paginado, filtros por `?tipo=` y `?estado=` |

## Ejecución

```bash
./mvnw spring-boot:run
```

El servidor corre en **http://localhost:8089**.

## Pruebas automatizadas

### Tests unitarios (JUnit + Mockito + JaCoCo)

```bash
./mvnw test
```

**32 tests** en 4 suites:
- `NotificacionServiceTest` — 8 casos (envío exitoso, 3 reintentos, asunto auto-generado, logs, filtros)
- `NotificacionControllerTest` — 12 casos (201, 500, 400 validación, logs con/sin filtros, tipo inválido)
- `EmailServiceTest` — 7 casos (5 templates renderizadas, envío con/sin adjunto PDF)
- `NotificacionesApplicationTests` — 5 casos (context load, enums, excepciones)

**Cobertura JaCoCo:** 92.8% instrucciones / 82.5% branches (reporte en `target/site/jacoco/index.html`).

### Tests de integración HTTP (todos los endpoints)

```bash
./http/run_tests.sh           # Ejecuta 13 requests, verifica códigos HTTP
./http/run_tests.sh --verbose # Muestra cuerpo de cada respuesta
```

Los 13 requests están documentados en [http/ms-notificaciones.http](http/ms-notificaciones.http), usables también manualmente desde VS Code con la extensión REST Client.

## Estructura de requests y respuestas

### POST /api/notificaciones/enviar — Confirmación de pago

```json
// Request
{
  "tipo": "CONFIRMACION_PAGO",
  "destinatario": "cliente@email.com",
  "asunto": null,
  "variables": {
    "nombre": "María López",
    "monto": "$45.990",
    "pedidoId": "#P-2026-001",
    "fecha": "15/01/2026"
  },
  "archivoAdjunto": null
}

// Response: 201 Created (con SMTP real)
// Response: 500 Internal Server Error (con SMTP dummy — ver configuración)
{
  "id": 1,
  "tipo": "CONFIRMACION_PAGO",
  "destinatario": "cliente@email.com",
  "asunto": "Confirmación de Pago — Perfulandia SPA",
  "estado": "ENVIADO",
  "intentos": 1,
  "fechaCreacion": "2026-06-23T15:30:00",
  "fechaEnvio": "2026-06-23T15:30:01",
  "error": null
}
```

**Validaciones:**
- `tipo` obligatorio: debe ser uno de los 5 tipos definidos (400 Bad Request si inválido)
- `destinatario` obligatorio: debe tener formato de email válido (400 Bad Request si no)
- `variables` obligatorio: mapa con las variables esperadas por la plantilla Thymeleaf
- `asunto` opcional: si es null, se genera automáticamente según el tipo de notificación

**Reglas de negocio:**
- Hasta 3 reintentos con backoff progresivo (1s, 2s, 3s). Si todos fallan → estado `FALLIDO`
- `FACTURA_EMITIDA` adjunta automáticamente un PDF dummy
- El registro se guarda en BD con estado `PENDIENTE` antes del primer intento de envío

### POST /api/notificaciones/enviar — Actualización de envío

```json
// Request
{
  "tipo": "ACTUALIZACION_ENVIO",
  "destinatario": "juan.perez@email.com",
  "asunto": null,
  "variables": {
    "nombre": "Juan Pérez",
    "pedidoId": "#E-2026-101",
    "estadoEnvio": "EN CAMINO",
    "linkSeguimiento": "https://perfulandia.cl/seguimiento/E-2026-101"
  },
  "archivoAdjunto": null
}
```

### POST /api/notificaciones/enviar — Factura emitida

```json
// Request
{
  "tipo": "FACTURA_EMITIDA",
  "destinatario": "empresa@cliente.cl",
  "asunto": "Factura #F-2026-050",
  "variables": {
    "nombre": "Empresa Ltda.",
    "monto": "$890.000",
    "pedidoId": "#F-2026-050",
    "fecha": "20/01/2026"
  },
  "archivoAdjunto": null
}

// Response: 201 Created
// Incluye PDF adjunto generado automáticamente como factura.pdf
```

### POST /api/notificaciones/enviar — Recuperación de clave

```json
// Request
{
  "tipo": "RECUPERACION_CLAVE",
  "destinatario": "pedro.gomez@email.com",
  "asunto": null,
  "variables": {
    "nombre": "Pedro Gómez",
    "enlaceRestablecimiento": "https://perfulandia.cl/restablecer?token=abc123"
  },
  "archivoAdjunto": null
}
```

### POST /api/notificaciones/enviar — Alerta del sistema

```json
// Request
{
  "tipo": "ALERTA_SISTEMA",
  "destinatario": "admin@perfulandia.cl",
  "asunto": null,
  "variables": {
    "nombreMicroservicio": "ms-pagos",
    "error": "Connection refused: connect — timeout after 30s",
    "timestamp": "2026-02-10T22:15:00"
  },
  "archivoAdjunto": null
}
```

### GET /api/notificaciones/logs — Consultar historial

```
GET /api/notificaciones/logs?page=0&size=20&sort=fechaCreacion,desc
GET /api/notificaciones/logs?tipo=CONFIRMACION_PAGO
GET /api/notificaciones/logs?estado=FALLIDO
GET /api/notificaciones/logs?tipo=ALERTA_SISTEMA&estado=ENVIADO

Response: 200 OK → Page<NotificacionResponseDTO>
```

Parámetros opcionales: `tipo`, `estado`, `page` (default 0), `size` (default 20), `sort` (default `fechaCreacion,desc`).

**Validaciones:**
- `tipo` debe ser un valor válido de `TipoNotificacion` (400 Bad Request si no)
- `estado` debe ser un valor válido de `EstadoNotificacion` (400 Bad Request si no)

## Configuración de base de datos

La aplicación usa MariaDB. La base de datos `perfulandia_notificaciones` se crea automáticamente (`createDatabaseIfNotExist=true`). Las tablas se crean vía Hibernate (`ddl-auto=update`). Los registros de prueba se insertan con `data.sql` al iniciar (`spring.sql.init.mode=always`).

Credenciales por defecto en [application.yml](src/main/resources/application.yml):
- Usuario: `root`
- Contraseña: `1234`

Para Duoc/XAMPP (MySQL nativo), usar el perfil `duoc`:
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=duoc
```

## Swagger / OpenAPI

Documentación interactiva disponible en:
- Swagger UI: http://localhost:8089/swagger-ui.html
- API Docs (JSON): http://localhost:8089/api-docs

## Actuator

- Health: http://localhost:8089/actuator/health
- Info: http://localhost:8089/actuator/info
- Metrics: http://localhost:8089/actuator/metrics
