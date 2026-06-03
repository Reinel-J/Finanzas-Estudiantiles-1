package com.finanzas.finanzas.repository;

import com.finanzas.finanzas.model.HistorialSemestre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistorialSemestreRepository extends JpaRepository<HistorialSemestre, Long> {

    List<HistorialSemestre> findByUsuarioIdOrderBySemestreNumeroDesc(Long usuarioId);

    List<HistorialSemestre> findByUsuarioIdAndSemestreNumero(Long usuarioId, Integer semestreNumero);
}
