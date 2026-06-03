package com.finanzas.finanzas.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * DTO para activar/actualizar el seguimiento de un producto en una meta.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeguimientoRequest {

    /** ID del producto en ML a vincular */
    private String externalProductId;

    /** Precio a partir del cual se quiere una alerta */
    private Double precioObjetivoCompra;

    /** true = activar, false = desactivar */
    private Boolean activo;
}
