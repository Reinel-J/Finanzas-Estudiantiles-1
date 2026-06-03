package com.finanzas.finanzas.controller;

import com.finanzas.finanzas.model.Gasto;
import com.finanzas.finanzas.service.GastoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/gastos")
@CrossOrigin(origins = "*")
public class GastoController {

    @Autowired
    private GastoService gastoService;

    // POST /api/gastos/{usuarioId}/ingreso/{ingresoId}
    @PostMapping("/{usuarioId}/ingreso/{ingresoId}")
    public ResponseEntity<?> registrar(
            @PathVariable Long usuarioId,
            @PathVariable Long ingresoId,
            @RequestBody Gasto gasto) {
        try {
            Gasto nuevo = gastoService.registrar(usuarioId, ingresoId, gasto);
            return ResponseEntity.ok(nuevo);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // POST /api/gastos/{usuarioId}  (sin asociar a ingreso)
    @PostMapping("/{usuarioId}")
    public ResponseEntity<?> registrarSimple(
            @PathVariable Long usuarioId,
            @RequestBody Gasto gasto) {
        try {
            Gasto nuevo = gastoService.registrarSimple(usuarioId, gasto);
            return ResponseEntity.ok(nuevo);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/gastos/{usuarioId}
    @GetMapping("/{usuarioId}")
    public ResponseEntity<List<Gasto>> listar(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(gastoService.listar(usuarioId));
    }

    // GET /api/gastos/{usuarioId}/semestre/{semestre}
    @GetMapping("/{usuarioId}/semestre/{semestre}")
    public ResponseEntity<List<Gasto>> listarPorSemestre(
            @PathVariable Long usuarioId,
            @PathVariable Integer semestre) {
        return ResponseEntity.ok(gastoService.listarPorSemestre(usuarioId, semestre));
    }

    // GET /api/gastos/{usuarioId}/mes-actual
    @GetMapping("/{usuarioId}/mes-actual")
    public ResponseEntity<List<Gasto>> listarMesActual(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(gastoService.listarMesActual(usuarioId));
    }

    // GET /api/gastos/{usuarioId}/semana?fecha=2026-04-22
    @GetMapping("/{usuarioId}/semana")
    public ResponseEntity<List<Gasto>> listarSemanales(
            @PathVariable Long usuarioId,
            @RequestParam(required = false) LocalDate fecha) {
        LocalDate fechaFin = fecha != null ? fecha : LocalDate.now();
        LocalDate fechaInicio = fechaFin.minusWeeks(1);
        return ResponseEntity.ok(gastoService.listarSemanales(usuarioId, fechaInicio, fechaFin));
    }

    // GET /api/gastos/{usuarioId}/total
    @GetMapping("/{usuarioId}/total")
    public ResponseEntity<Map<String, Double>> total(@PathVariable Long usuarioId) {
        Double total = gastoService.totalGastos(usuarioId);
        return ResponseEntity.ok(Map.of("total", total));
    }

    // GET /api/gastos/{usuarioId}/total/semestre/{semestre}
    @GetMapping("/{usuarioId}/total/semestre/{semestre}")
    public ResponseEntity<Map<String, Double>> totalPorSemestre(
            @PathVariable Long usuarioId,
            @PathVariable Integer semestre) {
        Double total = gastoService.totalPorSemestre(usuarioId, semestre);
        return ResponseEntity.ok(Map.of("total", total, "semestre", semestre.doubleValue()));
    }

    // GET /api/gastos/{usuarioId}/total/semana?fecha=2026-04-22
    @GetMapping("/{usuarioId}/total/semana")
    public ResponseEntity<Map<String, Double>> totalSemanales(
            @PathVariable Long usuarioId,
            @RequestParam(required = false) LocalDate fecha) {
        LocalDate fechaFin = fecha != null ? fecha : LocalDate.now();
        LocalDate fechaInicio = fechaFin.minusWeeks(1);
        Double total = gastoService.totalSemanales(usuarioId, fechaInicio, fechaFin);
        return ResponseEntity.ok(Map.of("total", total));
    }

    // GET /api/gastos/{usuarioId}/por-categoria
    @GetMapping("/{usuarioId}/por-categoria")
    public ResponseEntity<Map<String, Double>> porCategoria(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(gastoService.gastosPorCategoria(usuarioId));
    }

    // GET /api/gastos/{usuarioId}/resumen-semanal
    @GetMapping("/{usuarioId}/resumen-semanal")
    public ResponseEntity<Map<String, Object>> resumenSemanal(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(gastoService.resumenSemanal(usuarioId));
    }

    // GET /api/gastos/{usuarioId}/semanal/categoria?fecha=2026-04-22
    @GetMapping("/{usuarioId}/semanal/categoria")
    public ResponseEntity<Map<String, Double>> gastosSemanalesPorCategoria(
            @PathVariable Long usuarioId,
            @RequestParam(required = false) LocalDate fecha) {
        LocalDate fechaFin = fecha != null ? fecha : LocalDate.now();
        LocalDate fechaInicio = fechaFin.minusWeeks(1);
        return ResponseEntity.ok(gastoService.gastosSemanalesPorCategoria(usuarioId, fechaInicio, fechaFin));
    }

    // PUT /api/gastos/{id}
    @PutMapping("/{id}")
    public ResponseEntity<?> editar(@PathVariable Long id, @RequestBody Gasto datosNuevos) {
        try {
            Gasto actualizado = gastoService.editar(id, datosNuevos);
            return ResponseEntity.ok(actualizado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // DELETE /api/gastos/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            gastoService.eliminar(id);
            return ResponseEntity.ok(Map.of("mensaje", "Gasto eliminado correctamente."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}