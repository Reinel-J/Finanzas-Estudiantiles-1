package com.finanzas.finanzas.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Registra cada revisión de precio de un producto asociado a una MetaAhorro.
 * Permite graficar la evolución histórica del precio.
 */
@Entity
@Table(name = "historial_precios", indexes = {
        @Index(name = "idx_hp_meta", columnList = "meta_id"),
        @Index(name = "idx_hp_fecha", columnList = "fechaRevision")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistorialPrecio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Meta de ahorro a la que pertenece este registro */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meta_id", nullable = false)
    private MetaAhorro meta;

    /** Precio actual encontrado en el marketplace */
    @Column(nullable = false)
    private Double precio;

    /** Precio original (sin descuento). Puede ser null si no hay descuento */
    @Column(name = "precio_original")
    private Double precioOriginal;

    /** Porcentaje de descuento calculado */
    @Column(name = "descuento_porcentaje")
    private Double descuentoPorcentaje;

    /** Título del producto en el momento de la revisión */
    @Column(length = 512)
    private String tituloProducto;

    /** URL de la imagen del producto */
    @Column(name = "imagen_url", length = 1024)
    private String imagenUrl;

    /** Link directo al producto en el marketplace */
    @Column(name = "link_producto", length = 1024)
    private String linkProducto;

    /** Momento exacto en que se realizó la revisión */
    @Column(nullable = false)
    private LocalDateTime fechaRevision;

    /** Nombre del marketplace (MERCADOLIBRE, DUMMYJSON, etc.) */
    @Column(length = 50)
    private String marketplace;
}
