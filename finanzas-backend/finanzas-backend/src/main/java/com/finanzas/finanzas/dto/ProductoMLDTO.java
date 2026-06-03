package com.finanzas.finanzas.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * DTO que representa un producto encontrado en Mercado Libre.
 * Solo expone los campos necesarios para el frontend.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoMLDTO {

    /** ID único del producto en ML */
    private String id;

    /** Título del producto */
    private String titulo;

    /** Precio actual de venta */
    private Double precio;

    /** Precio original antes del descuento (puede ser null) */
    private Double precioOriginal;

    /** Porcentaje de descuento calculado */
    private Double descuentoPorcentaje;

    /** URL de la imagen principal */
    private String imagenUrl;

    /** Link al producto en la web de ML */
    private String link;

    /** Nombre de la tienda/vendedor */
    private String vendedor;

    /** Condición del producto: new, used */
    private String condicion;

    /** Moneda en que está expresado el precio (ej: COP) */
    private String moneda;
}
