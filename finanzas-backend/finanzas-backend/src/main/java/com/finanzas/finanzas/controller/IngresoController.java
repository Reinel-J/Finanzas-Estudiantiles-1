package com.finanzas.finanzas.controller;

import com.finanzas.finanzas.model.Ingreso;
import com.finanzas.finanzas.service.IngresoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ingresos")
@CrossOrigin(origins = "*")
public class IngresoController {

    @Autowired
    private IngresoService ingresoService;

    // POST /api/ingresos/{usuarioId}
    @PostMapping("/{usuarioId}")
    public ResponseEntity<?> agregar(@PathVariable Long usuarioId, @RequestBody Ingreso ingreso) {
        try {
            Ingreso nuevo = ingresoService.agregar(usuarioId, ingreso);
            return ResponseEntity.ok(nuevo);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/ingresos/{usuarioId}
    @GetMapping("/{usuarioId}")
    public ResponseEntity<List<Ingreso>> listar(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(ingresoService.listar(usuarioId));
    }

    // GET /api/ingresos/{usuarioId}/semestre/{semestre}
    @GetMapping("/{usuarioId}/semestre/{semestre}")
    public ResponseEntity<List<Ingreso>> listarPorSemestre(
            @PathVariable Long usuarioId,
            @PathVariable Integer semestre) {
        return ResponseEntity.ok(ingresoService.listarPorSemestre(usuarioId, semestre));
    }

    // GET /api/ingresos/{usuarioId}/mes-actual
    @GetMapping("/{usuarioId}/mes-actual")
    public ResponseEntity<List<Ingreso>> listarMesActual(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(ingresoService.listarMesActual(usuarioId));
    }

    // GET /api/ingresos/{usuarioId}/semana?fecha=2026-04-22
    @GetMapping("/{usuarioId}/semana")
    public ResponseEntity<List<Ingreso>> listarSemanales(
            @PathVariable Long usuarioId,
            @RequestParam(required = false) LocalDate fecha) {
        LocalDate fechaFin = fecha != null ? fecha : LocalDate.now();
        LocalDate fechaInicio = fechaFin.minusWeeks(1);
        return ResponseEntity.ok(ingresoService.listarSemanales(usuarioId, fechaInicio, fechaFin));
    }

    // GET /api/ingresos/{usuarioId}/total
    @GetMapping("/{usuarioId}/total")
    public ResponseEntity<Map<String, Double>> total(@PathVariable Long usuarioId) {
        Double total = ingresoService.totalIngresos(usuarioId);
        return ResponseEntity.ok(Map.of("total", total));
    }

    // GET /api/ingresos/{usuarioId}/total/semestre/{semestre}
    @GetMapping("/{usuarioId}/total/semestre/{semestre}")
    public ResponseEntity<Map<String, Double>> totalPorSemestre(
            @PathVariable Long usuarioId,
            @PathVariable Integer semestre) {
        Double total = ingresoService.totalPorSemestre(usuarioId, semestre);
        return ResponseEntity.ok(Map.of("total", total, "semestre", semestre.doubleValue()));
    }

    // PUT /api/ingresos/{id}
    @PutMapping("/{id}")
    public ResponseEntity<?> editar(@PathVariable Long id, @RequestBody Ingreso datosNuevos) {
        try {
            Ingreso actualizado = ingresoService.editar(id, datosNuevos);
            return ResponseEntity.ok(actualizado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // DELETE /api/ingresos/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            ingresoService.eliminar(id);
            return ResponseEntity.ok(Map.of("mensaje", "Ingreso eliminado correctamente."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}