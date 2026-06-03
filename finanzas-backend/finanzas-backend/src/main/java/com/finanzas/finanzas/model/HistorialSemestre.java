package com.finanzas.finanzas.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "historial_semestres")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistorialSemestre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "semestre_numero", nullable = false)
    private Integer semestreNumero;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(name = "fecha_finalizacion")
    private LocalDateTime fechaFinalizacion;

    @Column(name = "total_ingresos")
    private Double totalIngresos = 0.0;

    @Column(name = "total_gastos")
    private Double totalGastos = 0.0;

    @Column(name = "saldo_final")
    private Double saldoFinal = 0.0;

    @Column(name = "monto_ahorrado_final")
    private Double montoAhorradoFinal = 0.0;

    @Column(name = "cantidad_ingresos")
    private Integer cantidadIngresos = 0;

    @Column(name = "cantidad_gastos")
    private Integer cantidadGastos = 0;

    @Column(name = "gastos_por_categoria", columnDefinition = "TEXT")
    private String gastosPorCategoria;

    @PrePersist
    protected void onCreate() {
        if (fechaFinalizacion == null) {
            fechaFinalizacion = LocalDateTime.now();
        }
    }
}
