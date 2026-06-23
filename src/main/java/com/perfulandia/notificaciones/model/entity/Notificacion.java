package com.perfulandia.notificaciones.model.entity;

import com.perfulandia.notificaciones.model.enums.EstadoNotificacion;
import com.perfulandia.notificaciones.model.enums.TipoNotificacion;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notificaciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 50)
    private TipoNotificacion tipo;

    @Column(name = "destinatario", nullable = false, length = 150)
    private String destinatario;

    @Column(name = "asunto", nullable = false, length = 200)
    private String asunto;

    @Column(name = "cuerpo", nullable = false, columnDefinition = "TEXT")
    private String cuerpo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 50)
    @Builder.Default
    private EstadoNotificacion estado = EstadoNotificacion.PENDIENTE;

    @Column(name = "intentos", nullable = false)
    @Builder.Default
    private Integer intentos = 0;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;

    @Column(name = "error", columnDefinition = "TEXT")
    private String error;
}
