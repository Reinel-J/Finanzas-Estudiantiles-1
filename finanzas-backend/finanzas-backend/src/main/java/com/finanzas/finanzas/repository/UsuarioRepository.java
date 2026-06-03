package com.finanzas.finanzas.repository;

import com.finanzas.finanzas.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Buscar usuario por nombre (para login)
    Optional<Usuario> findByNombre(String nombre);

    // Verificar si ya existe un usuario con ese nombre
    boolean existsByNombre(String nombre);

    // Verificar si ya existe un usuario con ese número de documento
    boolean existsBynDoc(String nDoc);
}
