package com.finanzas.finanzas.repository;

import com.finanzas.finanzas.model.MetaAhorro;
import com.finanzas.finanzas.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MetaAhorroRepository extends JpaRepository<MetaAhorro, Long> {

    // Todas las metas de un usuario
    List<MetaAhorro> findByUsuario(Usuario usuario);

    // Metas en progreso
    List<MetaAhorro> findByUsuarioAndEstado(Usuario usuario, String estado);
}
