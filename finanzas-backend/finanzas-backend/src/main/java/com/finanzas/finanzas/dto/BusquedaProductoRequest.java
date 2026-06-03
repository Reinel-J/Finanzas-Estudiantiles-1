package com.finanzas.finanzas.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * DTO para solicitar la búsqueda o vinculación de un producto
 * a una meta de ahorro.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusquedaProductoRequest {

    /** Término de búsqueda libre (ej: "laptop gamer", "iPhone 15") */
    private String query;

    /** Precio máximo de compra que el usuario espera */
    private Double precioObjetivoCompra;
}
