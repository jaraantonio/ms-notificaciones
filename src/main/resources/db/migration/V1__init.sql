-- ═══════════════════════════════════════════════════════════════
-- Flyway V1__init.sql
-- Creación inicial de la tabla notificaciones
-- Evidencia para entorno DUOC (MySQL 8.x con Flyway activado)
-- En local: Flyway está desactivado (ddl-auto=update en MariaDB)
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS notificaciones (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    tipo            VARCHAR(50)     NOT NULL,
    destinatario    VARCHAR(150)    NOT NULL,
    asunto          VARCHAR(200)    NOT NULL,
    cuerpo          TEXT            NOT NULL,
    estado          VARCHAR(50)     NOT NULL DEFAULT 'PENDIENTE',
    intentos        INT             NOT NULL DEFAULT 0,
    fecha_creacion  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_envio     DATETIME        NULL,
    error           TEXT            NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Índices para búsquedas frecuentes
CREATE INDEX idx_notificaciones_estado ON notificaciones (estado);
CREATE INDEX idx_notificaciones_tipo ON notificaciones (tipo);
CREATE INDEX idx_notificaciones_fecha_creacion ON notificaciones (fecha_creacion);
