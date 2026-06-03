package com.finanzas.finanzas.controller;

import com.finanzas.finanzas.model.MetaAhorro;
import com.finanzas.finanzas.service.MetaAhorroService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/metas")
@CrossOrigin(origins = "*")
public class MetaAhorroController {

    @Autowired
    private MetaAhorroService metaAhorroService;

    // POST /api/metas/{usuarioId}
    @PostMapping("/{usuarioId}")
    public ResponseEntity<?> crear(@PathVariable Long usuarioId, @RequestBody MetaAhorro meta) {
        try {
            MetaAhorro nueva = metaAhorroService.crear(usuarioId, meta);
            return ResponseEntity.ok(nueva);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/metas/{usuarioId}
    @GetMapping("/{usuarioId}")
    public ResponseEntity<List<MetaAhorro>> listar(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(metaAhorroService.listar(usuarioId));
    }

    // PUT /api/metas/{id}/abonar
    @PutMapping("/{id}/abonar")
    public ResponseEntity<?> abonar(@PathVariable Long id, @RequestBody Map<String, Double> body) {
        try {
            Double monto = body.get("monto");
            MetaAhorro actualizada = metaAhorroService.abonar(id, monto);
            double progreso = metaAhorroService.calcularProgreso(actualizada);
            return ResponseEntity.ok(Map.of("meta", actualizada, "progreso", progreso));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // PUT /api/metas/{id}
    @PutMapping("/{id}")
    public ResponseEntity<?> editar(@PathVariable Long id, @RequestBody MetaAhorro datosNuevos) {
        try {
            MetaAhorro actualizada = metaAhorroService.editar(id, datosNuevos);
            return ResponseEntity.ok(actualizada);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // PUT /api/metas/{id}/cancelar
    @PutMapping("/{id}/cancelar")
    public ResponseEntity<?> cancelar(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(metaAhorroService.cancelar(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // DELETE /api/metas/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            metaAhorroService.eliminar(id);
            return ResponseEntity.ok(Map.of("mensaje", "Meta eliminada correctamente."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/metas/{usuarioId}/descuentos
    @GetMapping("/{usuarioId}/descuentos")
    public ResponseEntity<?> verificarDescuentos(@PathVariable Long usuarioId) {
        try {
            return ResponseEntity.ok(metaAhorroService.verificarDescuentos(usuarioId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
