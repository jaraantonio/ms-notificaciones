# Microservicio de Notificaciones (ms-notificaciones)

## Descripción del Proyecto
Microservicio responsable de la orquestación, envío y seguimiento de alertas y comunicados a los usuarios del sistema.

## Estudiante
* Antonio Jara

## Funcionalidades Implementadas
- Gestión y envío de notificaciones mediante plantillas HTML.
- Trazabilidad del estado de cada notificación (Pendiente, Enviada, Fallida).
- Clasificación por tipo de notificación.
- Integración mediante cliente Feign/REST con el servicio de usuarios.

## Pasos para Ejecutar

1. **Requisitos previos:**
   * Java Development Kit (JDK) 17 o superior.
   * Apache Maven.
   * Base de datos operativa y configurada en `application.properties`.

2. **Compilación del proyecto:**
   Abre una terminal en la raíz del proyecto y ejecuta:
   ```bash
   ./mvnw clean install
   ```
   *(En Windows utiliza `mvnw.cmd clean install`)*

3. **Ejecución del servicio:**
   Levanta la aplicación mediante Spring Boot:
   ```bash
   ./mvnw spring-boot:run
   ```
   *(En Windows utiliza `mvnw.cmd spring-boot:run`)*

## APIs para Pruebas en Postman

**Consideración de Seguridad:** Las rutas de este servicio interactúan principalmente de forma inter-servicio o protegidas por el API Gateway corporativo. Al consumir directo se debe adjuntar el Bearer Token generado desde el login en el Header `Authorization`.

* **Enviar Notificación**
    * **Método:** `POST`
    * **Ruta:** `/api/notificaciones`
    * **Requiere Token:** Sí (Cualquier usuario/sistema autenticado)
    * **Payload (JSON crudo):**
        ```json
        {
          "idUsuario": 1,
          "destinatario": "cliente@email.com",
          "asunto": "Confirmación de Envío",
          "cuerpoMensaje": "Tu pedido ha sido procesado con éxito.",
          "tipo": "PEDIDO"
        }
        ```

* **Verificar Salud de Integración con Usuarios**
    * **Método:** `GET`
    * **Ruta:** `/api/notificaciones/health-check-usuarios`
    * **Requiere Token:** No (Ruta interna de monitoreo)