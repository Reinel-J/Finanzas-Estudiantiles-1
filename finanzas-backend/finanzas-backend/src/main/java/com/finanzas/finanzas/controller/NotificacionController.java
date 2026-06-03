package com.finanzas.finanzas.controller;

import com.finanzas.finanzas.model.Notificacion;
import com.finanzas.finanzas.model.Usuario;
import com.finanzas.finanzas.repository.NotificacionRepository;
import com.finanzas.finanzas.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/notificaciones")
@CrossOrigin(origins = "*")
public class NotificacionController {

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    // GET /api/notificaciones/{usuarioId}
    @GetMapping("/{usuarioId}")
    public ResponseEntity<?> obtenerNotificaciones(@PathVariable Long usuarioId) {
        try {
            Optional<Usuario> usuarioOpt = usuarioRepository.findById(usuarioId);
            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            List<Notificacion> notificaciones = notificacionRepository.findTop50ByUsuarioOrderByFechaDesc(usuarioOpt.get());
            return ResponseEntity.ok(notificaciones);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/notificaciones/{usuarioId}/no-leidas/count
    @GetMapping("/{usuarioId}/no-leidas/count")
    public ResponseEntity<?> contarNoLeidas(@PathVariable Long usuarioId) {
        try {
            Optional<Usuario> usuarioOpt = usuarioRepository.findById(usuarioId);
            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            long count = notificacionRepository.countByUsuarioAndLeidaFalse(usuarioOpt.get());
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // PUT /api/notificaciones/{id}/leer
    @PutMapping("/{id}/leer")
    public ResponseEntity<?> marcarComoLeida(@PathVariable Long id) {
        try {
            Optional<Notificacion> notifOpt = notificacionRepository.findById(id);
            if (notifOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            Notificacion notif = notifOpt.get();
            notif.setLeida(true);
            notificacionRepository.save(notif);
            return ResponseEntity.ok(notif);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // PUT /api/notificaciones/{usuarioId}/leer-todas
    @PutMapping("/{usuarioId}/leer-todas")
    public ResponseEntity<?> marcarTodasComoLeidas(@PathVariable Long usuarioId) {
        try {
            Optional<Usuario> usuarioOpt = usuarioRepository.findById(usuarioId);
            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            notificacionRepository.marcarTodasLeidas(usuarioOpt.get());
            return ResponseEntity.ok(Map.of("mensaje", "Todas las notificaciones marcadas como leídas."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
