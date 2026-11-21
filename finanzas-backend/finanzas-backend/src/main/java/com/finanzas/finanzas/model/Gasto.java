package com.finanzas.finanzas.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "gastos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Gasto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación con el usuario
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // Relación con el ingreso del que se descuenta
    @ManyToOne
    @JoinColumn(name = "ingreso_id")
    private Ingreso ingreso;

    @Column(nullable = false)
    private Double monto;

    @Column
    private String descripcion;

    @Column(nullable = false)
    private LocalDate fecha;

    // Nueva funcionalidad: categorías
    @Column
    private String categoria; // ej: "Alimentación", "Transporte", "Educación", "Otro"

    // ── Semestre ────────────────────────────────────────────────
    @Column
    private Integer semestre; // ej: 1, 2, 3, etc.
}
