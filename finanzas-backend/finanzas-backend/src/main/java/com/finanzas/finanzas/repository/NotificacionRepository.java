package com.finanzas.finanzas.repository;

import com.finanzas.finanzas.model.Notificacion;
import com.finanzas.finanzas.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    /** Todas las notificaciones de un usuario, más recientes primero */
    List<Notificacion> findByUsuarioOrderByFechaDesc(Usuario usuario);

    /** Solo las no leídas */
    List<Notificacion> findByUsuarioAndLeidaFalseOrderByFechaDesc(Usuario usuario);

    /** Contar no leídas (para badge en el frontend) */
    long countByUsuarioAndLeidaFalse(Usuario usuario);

    /** Últimas 50 notificaciones de un usuario */
    List<Notificacion> findTop50ByUsuarioOrderByFechaDesc(Usuario usuario);

    /** Marcar todas como leídas para un usuario */
    @Modifying
    @Transactional
    @Query("UPDATE Notificacion n SET n.leida = true WHERE n.usuario = :usuario")
    void marcarTodasLeidas(@Param("usuario") Usuario usuario);
}
