package com.finanzas.finanzas.repository;

import com.finanzas.finanzas.model.Ingreso;
import com.finanzas.finanzas.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface IngresoRepository extends JpaRepository<Ingreso, Long> {

    // Todos los ingresos de un usuario ordenados por fecha
    List<Ingreso> findByUsuarioOrderByFechaDesc(Usuario usuario);

    // Suma total de ingresos de un usuario
    @Query("SELECT COALESCE(SUM(i.monto), 0) FROM Ingreso i WHERE i.usuario = :usuario")
    Double sumMontoByUsuario(@Param("usuario") Usuario usuario);

    // Ingresos por categoría
    List<Ingreso> findByUsuarioAndCategoria(Usuario usuario, String categoria);

    // Ingresos del mes actual
    @Query("SELECT i FROM Ingreso i WHERE i.usuario = :usuario " +
           "AND MONTH(i.fecha) = MONTH(CURRENT_DATE) " +
           "AND YEAR(i.fecha) = YEAR(CURRENT_DATE)")
    List<Ingreso> findIngresosMesActual(@Param("usuario") Usuario usuario);

    // ── Filtrado por semestre ─────────────────────────────────

    List<Ingreso> findByUsuarioAndSemestreOrderByFechaDesc(Usuario usuario, Integer semestre);

    @Query("SELECT COALESCE(SUM(i.monto), 0) FROM Ingreso i WHERE i.usuario = :usuario AND i.semestre = :semestre")
    Double sumMontoByUsuarioAndSemestre(@Param("usuario") Usuario usuario, @Param("semestre") Integer semestre);

    // Ingresos del semestre entre fechas
    @Query("SELECT i FROM Ingreso i WHERE i.usuario = :usuario " +
           "AND i.semestre = :semestre " +
           "AND i.fecha BETWEEN :fechaInicio AND :fechaFin " +
           "ORDER BY i.fecha DESC")
    List<Ingreso> findByUsuarioAndSemestreAndFechaBetween(
            @Param("usuario") Usuario usuario,
            @Param("semestre") Integer semestre,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin);

    // ── Filtrado semanal ──────────────────────────────────────

    @Query("SELECT i FROM Ingreso i WHERE i.usuario = :usuario " +
           "AND i.fecha BETWEEN :fechaInicio AND :fechaFin " +
           "ORDER BY i.fecha DESC")
    List<Ingreso> findIngresosSemanales(
            @Param("usuario") Usuario usuario,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin);
}
