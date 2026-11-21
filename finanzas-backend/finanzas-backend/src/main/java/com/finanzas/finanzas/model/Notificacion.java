package com.finanzas.finanzas.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Notificaciones internas del sistema hacia el usuario.
 * Se generan automáticamente cuando el scheduler detecta cambios de precio,
 * descuentos o cuando una meta alcanza su objetivo.
 */
@Entity
@Table(name = "notificaciones", indexes = {
        @Index(name = "idx_notif_usuario", columnList = "usuario_id"),
        @Index(name = "idx_notif_leida", columnList = "leida"),
        @Index(name = "idx_notif_fecha", columnList = "fecha")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Usuario destinatario de la notificación */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /** Título corto de la notificación (máx 255 chars) */
    @Column(nullable = false)
    private String titulo;

    /** Mensaje detallado */
    @Column(nullable = false, length = 1024)
    private String mensaje;

    /** Si el usuario ya la ha visto */
    @Column(nullable = false)
    private Boolean leida = false;

    /** Momento en que se generó la notificación */
    @Column(nullable = false)
    private LocalDateTime fecha;

    /**
     * Tipo de notificación para diferenciar íconos y acciones en el frontend.
     * Valores: DESCUENTO, PRECIO_BAJO, META_COMPLETADA, PRECIO_SUBIO, SEGUIMIENTO
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoNotificacion tipo = TipoNotificacion.SEGUIMIENTO;

    /** ID de la meta relacionada (para poder navegar a ella desde la notif) */
    @Column(name = "meta_id")
    private Long metaId;

    public enum TipoNotificacion {
        DESCUENTO,       // Hay un descuento activo
        PRECIO_BAJO,     // El precio bajó respecto al histórico
        PRECIO_SUBIO,    // El precio subió
        META_COMPLETADA, // La meta de ahorro se completó
        SEGUIMIENTO      // Actualización general de seguimiento
    }
}
