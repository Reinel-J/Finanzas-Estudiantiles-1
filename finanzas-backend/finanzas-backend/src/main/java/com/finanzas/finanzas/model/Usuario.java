package com.finanzas.finanzas.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

@Entity
@Table(name = "usuarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {
    // crea la base de datos (registros de usuarios)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    @Column(nullable = false)
    private String apellido;

    @Column(nullable = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String contrasena;

    @Column(name = "tipo_doc", nullable = false)
    @JsonProperty("tipoDoc")
    private String tipoDoc;

    @Column(name = "n_doc", nullable = false, unique = true)
    @JsonProperty("nDoc")
    private String nDoc;

    @Column(nullable = false)
    private String universidad;

    // ── Semestre ────────────────────────────────────────────────
    @Column(name = "semestre_actual")
    private Integer semestreActual = 1;

    @Column(name = "fecha_inicio_semestre")
    private LocalDate fechaInicioSemestre;

    @Column(name = "fecha_fin_semestre")
    private LocalDate fechaFinSemestre;

    // ── Ahorro automático ───────────────────────────────────────
    @Column(name = "porcentaje_ahorro")
    private Double porcentajeAhorro = 10.0; // Porcentaje default 10%

    @Column(name = "monto_ahorrado")
    private Double montoAhorrado = 0.0; // Acumulado de ahorros

    // ── Getters seguros para evitar NullPointerException ────────
    public Integer getSemestreActual() {
        return semestreActual != null ? semestreActual : 1;
    }

    public Double getPorcentajeAhorro() {
        return porcentajeAhorro != null ? porcentajeAhorro : 10.0;
    }

    public Double getMontoAhorrado() {
        return montoAhorrado != null ? montoAhorrado : 0.0;
    }
}
