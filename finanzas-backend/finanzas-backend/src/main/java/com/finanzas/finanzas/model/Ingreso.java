package com.finanzas.finanzas.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "ingresos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Ingreso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación con el usuario dueño del ingreso
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private Double monto;

    @Column
    private String descripcion;

    @Column(nullable = false)
    private LocalDate fecha;

    // Nueva funcionalidad: categorías
    @Column
    private String categoria; // ej: "Beca", "Trabajo", "Familia", "Otro"

    // ── Semestre ────────────────────────────────────────────────
    @Column(name = "semestre")
    private Integer semestre; // ej: 1, 2, 3, etc.
}
