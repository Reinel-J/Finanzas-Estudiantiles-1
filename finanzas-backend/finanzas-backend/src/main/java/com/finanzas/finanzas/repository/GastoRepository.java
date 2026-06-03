package com.finanzas.finanzas.repository;

import com.finanzas.finanzas.model.Gasto;
import com.finanzas.finanzas.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface GastoRepository extends JpaRepository<Gasto, Long> {

    // Todos los gastos de un usuario ordenados por fecha
    List<Gasto> findByUsuarioOrderByFechaDesc(Usuario usuario);

    // Suma total de gastos de un usuario
    @Query("SELECT COALESCE(SUM(g.monto), 0) FROM Gasto g WHERE g.usuario = :usuario")
    Double sumMontoByUsuario(@Param("usuario") Usuario usuario);

    // Gastos por categoría de un usuario
    List<Gasto> findByUsuarioAndCategoria(Usuario usuario, String categoria);

    // Suma de gastos por categoría (para gráficas)
    @Query("SELECT g.categoria, SUM(g.monto) FROM Gasto g " +
           "WHERE g.usuario = :usuario GROUP BY g.categoria")
    List<Object[]> sumGastosPorCategoria(@Param("usuario") Usuario usuario);

    // Gastos del mes actual
    @Query("SELECT g FROM Gasto g WHERE g.usuario = :usuario " +
           "AND MONTH(g.fecha) = MONTH(CURRENT_DATE) " +
           "AND YEAR(g.fecha) = YEAR(CURRENT_DATE)")
    List<Gasto> findGastosMesActual(@Param("usuario") Usuario usuario);

    // ── Filtrado por semestre ─────────────────────────────────

    List<Gasto> findByUsuarioAndSemestreOrderByFechaDesc(Usuario usuario, Integer semestre);

    @Query("SELECT COALESCE(SUM(g.monto), 0) FROM Gasto g WHERE g.usuario = :usuario AND g.semestre = :semestre")
    Double sumMontoByUsuarioAndSemestre(@Param("usuario") Usuario usuario, @Param("semestre") Integer semestre);

    // Gastos del semestre entre fechas
    @Query("SELECT g FROM Gasto g WHERE g.usuario = :usuario " +
           "AND g.semestre = :semestre " +
           "AND g.fecha BETWEEN :fechaInicio AND :fechaFin " +
           "ORDER BY g.fecha DESC")
    List<Gasto> findByUsuarioAndSemestreAndFechaBetween(
            @Param("usuario") Usuario usuario,
            @Param("semestre") Integer semestre,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin);

    // ── Filtrado semanal ──────────────────────────────────────

    @Query("SELECT g FROM Gasto g WHERE g.usuario = :usuario " +
           "AND g.fecha BETWEEN :fechaInicio AND :fechaFin " +
           "ORDER BY g.fecha DESC")
    List<Gasto> findGastosSemanales(
            @Param("usuario") Usuario usuario,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin);

    // Gastos semanales por categoría
    @Query("SELECT g.categoria, SUM(g.monto) FROM Gasto g " +
           "WHERE g.usuario = :usuario AND g.fecha BETWEEN :fechaInicio AND :fechaFin " +
           "GROUP BY g.categoria")
    List<Object[]> sumGastosSemanalesPorCategoria(
            @Param("usuario") Usuario usuario,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin);
}
