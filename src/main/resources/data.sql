-- ═══════════════════════════════════════════════════════════════
-- Seed Data — Registros de ejemplo para todos los tipos y estados
-- INSERT IGNORE garantiza idempotencia (no falla en ejecuciones repetidas)
-- Fechas fijas para consistencia en demostraciones
-- ═══════════════════════════════════════════════════════════════

-- ─── CONFIRMACION_PAGO ───
INSERT IGNORE INTO notificaciones (id, tipo, destinatario, asunto, cuerpo, estado, intentos, fecha_creacion, fecha_envio, error) VALUES
(1, 'CONFIRMACION_PAGO', 'maria.lopez@email.com',
 'Confirmación de Pago - Pedido #P-2026-001',
 '<!DOCTYPE html><html lang="es"><head><meta charset="UTF-8"><title>Confirmación de Pago</title></head><body style="font-family:Arial,sans-serif;color:#333;line-height:1.6;"><div style="max-width:600px;margin:0 auto;padding:20px;border:1px solid #ddd;border-radius:8px;"><h2 style="color:#2d6a4f;">¡Gracias por tu compra, María!</h2><p>Tu pago por <strong>$45.990</strong> para el <strong>Pedido #P-2026-001</strong> ha sido confirmado con éxito.</p><p>Fecha: 15/01/2026 10:30</p><hr style="border:0;border-top:1px solid #eee;margin:20px 0;"><p style="font-size:12px;color:#777;"><strong>Equipo Perfulandia SPA</strong><br><em>Este es un correo generado automáticamente, no responda a esta dirección.</em></p></div></body></html>',
 'ENVIADO', 1, '2026-01-15 10:30:00', '2026-01-15 10:30:05', NULL);

INSERT IGNORE INTO notificaciones (id, tipo, destinatario, asunto, cuerpo, estado, intentos, fecha_creacion, fecha_envio, error) VALUES
(2, 'CONFIRMACION_PAGO', 'carlos.ruiz@email.com',
 'Confirmación de Pago - Pedido #P-2026-002',
 '<!DOCTYPE html>...',
 'PENDIENTE', 0, '2026-01-15 14:20:00', NULL, NULL);

-- ─── ACTUALIZACION_ENVIO ───
INSERT IGNORE INTO notificaciones (id, tipo, destinatario, asunto, cuerpo, estado, intentos, fecha_creacion, fecha_envio, error) VALUES
(3, 'ACTUALIZACION_ENVIO', 'juan.perez@email.com',
 'Actualización de Envío - Pedido #E-2026-101',
 '<!DOCTYPE html><html lang="es"><head><meta charset="UTF-8"><title>Actualización de Envío</title></head><body style="font-family:Arial,sans-serif;color:#333;line-height:1.6;"><div style="max-width:600px;margin:0 auto;padding:20px;border:1px solid #ddd;border-radius:8px;"><h2 style="color:#1b4332;">Actualización de tu envío</h2><p>Hola <strong>Juan</strong>,</p><p>Tu pedido <strong>#E-2026-101</strong> ha cambiado de estado: <span style="color:#2d6a4f;font-weight:bold;">EN CAMINO</span></p><p>Sigue tu pedido en: <a href="https://perfulandia.cl/seguimiento/E-2026-101" style="color:#40916c;">Ver seguimiento</a></p><hr style="border:0;border-top:1px solid #eee;margin:20px 0;"><p style="font-size:12px;color:#777;"><strong>Equipo Perfulandia SPA</strong><br><em>Este es un correo generado automáticamente, no responda a esta dirección.</em></p></div></body></html>',
 'ENVIADO', 1, '2026-01-16 09:15:00', '2026-01-16 09:15:03', NULL);

INSERT IGNORE INTO notificaciones (id, tipo, destinatario, asunto, cuerpo, estado, intentos, fecha_creacion, fecha_envio, error) VALUES
(4, 'ACTUALIZACION_ENVIO', 'ana.martinez@email.com',
 'Actualización de Envío - Pedido #E-2026-102',
 '<!DOCTYPE html>...',
 'ENVIADO', 1, '2026-01-16 11:00:00', '2026-01-16 11:00:02', NULL);

-- ─── FACTURA_EMITIDA ───
INSERT IGNORE INTO notificaciones (id, tipo, destinatario, asunto, cuerpo, estado, intentos, fecha_creacion, fecha_envio, error) VALUES
(5, 'FACTURA_EMITIDA', 'empresa@cliente-corporativo.cl',
 'Factura #F-2026-050 — Perfulandia SPA',
 '<!DOCTYPE html><html lang="es"><head><meta charset="UTF-8"><title>Factura Emitida</title></head><body style="font-family:Arial,sans-serif;color:#333;line-height:1.6;"><div style="max-width:600px;margin:0 auto;padding:20px;border:1px solid #ddd;border-radius:8px;"><h2 style="color:#1b4332;">Factura Emitida</h2><p>Estimado cliente,</p><p>Se ha emitido la <strong>Factura #F-2026-050</strong> por un monto de <strong>$890.000</strong> con fecha <strong>20/01/2026</strong>.</p><p>El comprobante en PDF se adjunta a este correo.</p><hr style="border:0;border-top:1px solid #eee;margin:20px 0;"><p style="font-size:12px;color:#777;"><strong>Equipo Perfulandia SPA</strong><br><em>Este es un correo generado automáticamente, no responda a esta dirección.</em></p></div></body></html>',
 'ENVIADO', 1, '2026-01-20 08:00:00', '2026-01-20 08:00:04', NULL);

INSERT IGNORE INTO notificaciones (id, tipo, destinatario, asunto, cuerpo, estado, intentos, fecha_creacion, fecha_envio, error) VALUES
(6, 'FACTURA_EMITIDA', 'facturacion@servicios-ltda.cl',
 'Factura #F-2026-051 — Perfulandia SPA',
 '<!DOCTYPE html>...',
 'FALLIDO', 3, '2026-01-20 08:05:00', NULL,
 'jakarta.mail.SendFailedException: 550 5.1.1 User unknown (intento 3/3)');

-- ─── RECUPERACION_CLAVE ───
INSERT IGNORE INTO notificaciones (id, tipo, destinatario, asunto, cuerpo, estado, intentos, fecha_creacion, fecha_envio, error) VALUES
(7, 'RECUPERACION_CLAVE', 'pedro.gomez@email.com',
 'Recuperación de Clave — Perfulandia SPA',
 '<!DOCTYPE html><html lang="es"><head><meta charset="UTF-8"><title>Recuperación de Clave</title></head><body style="font-family:Arial,sans-serif;color:#333;line-height:1.6;"><div style="max-width:600px;margin:0 auto;padding:20px;border:1px solid #ddd;border-radius:8px;"><h2 style="color:#2d6a4f;">Recuperación de Clave</h2><p>Hola <strong>Pedro</strong>,</p><p>Hemos recibido una solicitud para restablecer tu contraseña.</p><p style="text-align:center;"><a href="https://perfulandia.cl/restablecer?token=abc123def456" style="background:#40916c;color:#fff;padding:12px 24px;text-decoration:none;border-radius:4px;display:inline-block;">Restablecer Contraseña</a></p><p>Este enlace expira en 15 minutos. Si no solicitaste este cambio, ignora este correo.</p><hr style="border:0;border-top:1px solid #eee;margin:20px 0;"><p style="font-size:12px;color:#777;"><strong>Equipo Perfulandia SPA</strong><br><em>Este es un correo generado automáticamente, no responda a esta dirección.</em></p></div></body></html>',
 'ENVIADO', 1, '2026-02-01 16:45:00', '2026-02-01 16:45:02', NULL);

INSERT IGNORE INTO notificaciones (id, tipo, destinatario, asunto, cuerpo, estado, intentos, fecha_creacion, fecha_envio, error) VALUES
(8, 'RECUPERACION_CLAVE', 'laura.soto@email.com',
 'Recuperación de Clave — Perfulandia SPA',
 '<!DOCTYPE html>...',
 'PENDIENTE', 0, '2026-02-02 10:10:00', NULL, NULL);

-- ─── ALERTA_SISTEMA ───
INSERT IGNORE INTO notificaciones (id, tipo, destinatario, asunto, cuerpo, estado, intentos, fecha_creacion, fecha_envio, error) VALUES
(9, 'ALERTA_SISTEMA', 'admin@perfulandia.cl',
 'ALERTA CRÍTICA: ms-pagos no responde',
 '<!DOCTYPE html><html lang="es"><head><meta charset="UTF-8"><title>Alerta del Sistema</title></head><body style="font-family:Arial,sans-serif;color:#333;line-height:1.6;"><div style="max-width:600px;margin:0 auto;padding:20px;border:1px solid #e63946;border-radius:8px;background:#fff5f5;"><h2 style="color:#e63946;">⚠ Alerta del Sistema</h2><p>El microservicio <strong>ms-pagos</strong> ha reportado un error:</p><pre style="background:#f8f9fa;padding:12px;border-radius:4px;overflow-x:auto;">Connection refused: connect</pre><p>Timestamp: <strong>2026-02-10T22:15:00</strong></p><p style="color:#e63946;font-weight:bold;">Se requiere intervención inmediata.</p><hr style="border:0;border-top:1px solid #eee;margin:20px 0;"><p style="font-size:12px;color:#777;"><strong>Equipo Perfulandia SPA</strong><br><em>Este es un correo generado automáticamente, no responda a esta dirección.</em></p></div></body></html>',
 'ENVIADO', 1, '2026-02-10 22:15:00', '2026-02-10 22:15:01', NULL);

INSERT IGNORE INTO notificaciones (id, tipo, destinatario, asunto, cuerpo, estado, intentos, fecha_creacion, fecha_envio, error) VALUES
(10, 'ALERTA_SISTEMA', 'admin@perfulandia.cl',
 'ALERTA CRÍTICA: ms-envios timeout',
 '<!DOCTYPE html>...',
 'FALLIDO', 3, '2026-02-11 03:30:00', NULL,
 'jakarta.mail.SendFailedException: 421 4.7.0 Temporary System Problem (intento 3/3)');
