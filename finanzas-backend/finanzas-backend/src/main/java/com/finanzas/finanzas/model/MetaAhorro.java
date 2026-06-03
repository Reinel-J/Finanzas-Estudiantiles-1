package com.finanzas.finanzas.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Meta de ahorro del usuario.
 * Puede vincularse a un producto real en Mercado Libre para seguimiento
 * automático de precios y detección de descuentos.
 */
@Entity
@Table(name = "metas_ahorro")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetaAhorro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Relación con el usuario propietario de la meta */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private String nombre; // ej: "Laptop", "Viaje", "Emergencias"

    @Column(name = "monto_objetivo", nullable = false)
    private Double montoObjetivo;

    @Column(name = "monto_actual")
    private Double montoActual = 0.0;

    @Column(name = "fecha_limite")
    private LocalDate fechaLimite;

    @Column
    private String estado; // "EN_PROGRESO", "COMPLETADA", "CANCELADA"

    @Column
    private Integer semestre;

    // ── Integración con marketplace (CAMPO LEGADO - DummyJSON) ──
    @Column(name = "id_producto_externo")
    private String idProductoExterno;

    // ── NUEVOS CAMPOS: Seguimiento inteligente Mercado Libre ────

    /**
     * Nombre del marketplace: MERCADOLIBRE, DUMMYJSON, MANUAL
     */
    @Column(length = 30)
    private String marketplace;

    /**
     * ID del producto en el marketplace (ej: "MCO123456789" en ML)
     */
    @Column(name = "external_product_id", length = 100)
    private String externalProductId;

    /**
     * URL directa al producto en el marketplace
     */
    @Column(name = "product_url", length = 1024)
    private String productUrl;

    /**
     * Título del producto en el marketplace
     */
    @Column(name = "product_title", length = 512)
    private String productTitle;

    /**
     * URL de la imagen del producto
     */
    @Column(name = "product_image_url", length = 1024)
    private String productImageUrl;

    /**
     * Último precio detectado en el marketplace
     */
    @Column(name = "ultimo_precio_detectado")
    private Double ultimoPrecioDetectado;

    /**
     * Precio original antes del descuento (si aplica)
     */
    @Column(name = "ultimo_precio_original")
    private Double ultimoPrecioOriginal;

    /**
     * Último porcentaje de descuento detectado
     */
    @Column(name = "ultimo_descuento_porcentaje")
    private Double ultimoDescuentoPorcentaje;

    /**
     * Precio objetivo que el usuario quiere esperar para comprar
     * (ej: "avísame cuando baje de $2.000.000")
     */
    @Column(name = "precio_objetivo_compra")
    private Double precioObjetivoCompra;

    /**
     * Indica si el sistema debe revisar precios automáticamente
     */
    @Column(name = "seguimiento_activo")
    private Boolean seguimientoActivo = false;

    /**
     * Última vez que se revisó el precio
     */
    @Column(name = "ultima_revision")
    private LocalDateTime ultimaRevision;

    /**
     * El precio más bajo que se ha detectado en el histórico
     */
    @Column(name = "mejor_precio_historico")
    private Double mejorPrecioHistorico;

    /**
     * Historial de precios - relación OneToMany (cascade para que
     * se eliminen al borrar la meta)
     */
    @OneToMany(mappedBy = "meta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HistorialPrecio> historialPrecios = new ArrayList<>();
}
