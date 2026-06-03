package com.finanzas.finanzas.repository;

import com.finanzas.finanzas.model.HistorialPrecio;
import com.finanzas.finanzas.model.MetaAhorro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HistorialPrecioRepository extends JpaRepository<HistorialPrecio, Long> {

    /** Historial completo de una meta, ordenado del más reciente al más antiguo */
    List<HistorialPrecio> findByMetaOrderByFechaRevisionDesc(MetaAhorro meta);

    /** Últimas N revisiones de una meta */
    List<HistorialPrecio> findTop30ByMetaOrderByFechaRevisionDesc(MetaAhorro meta);

    /** El precio mínimo registrado históricamente para una meta */
    @Query("SELECT MIN(h.precio) FROM HistorialPrecio h WHERE h.meta = :meta")
    Optional<Double> findMinPrecioByMeta(@Param("meta") MetaAhorro meta);

    /** Verificar si ya existe una revisión reciente (para no duplicar en el scheduler) */
    @Query("SELECT COUNT(h) > 0 FROM HistorialPrecio h WHERE h.meta = :meta " +
           "AND h.fechaRevision >= :desde")
    boolean existsRevisionReciente(
            @Param("meta") MetaAhorro meta,
            @Param("desde") java.time.LocalDateTime desde);
}
