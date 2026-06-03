package com.finanzas.finanzas.controller;

import com.finanzas.finanzas.model.Reporte;
import com.finanzas.finanzas.service.AnalisisIntegralService;
import com.finanzas.finanzas.service.ReporteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reportes")
@CrossOrigin(origins = "*")
public class ReporteController {

    @Autowired
    private ReporteService reporteService;

    @Autowired
    private AnalisisIntegralService analisisIntegralService;

    // GET /api/reportes/{usuarioId}/promedio-diario?fechaInicio=YYYY-MM-DD&fechaFin=YYYY-MM-DD
    @GetMapping("/{usuarioId}/promedio-diario")
    public ResponseEntity<?> obtenerPromedioDiarioIntegral(
            @PathVariable Long usuarioId,
            @RequestParam String fechaInicio,
            @RequestParam String fechaFin) {
        try {
            LocalDate inicio = LocalDate.parse(fechaInicio);
            LocalDate fin = LocalDate.parse(fechaFin);
            Map<String, Object> resultado = analisisIntegralService.calcularPromedioDiarioIntegral(
                    usuarioId, inicio, fin);
            return ResponseEntity.ok(resultado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // POST /api/reportes/{usuarioId}
    @PostMapping("/{usuarioId}")
    public ResponseEntity<?> guardarReporte(
            @PathVariable Long usuarioId,
            @RequestBody Reporte reporte) {
        try {
            Reporte nuevo = reporteService.guardarReporte(usuarioId, reporte);
            return ResponseEntity.ok(nuevo);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/reportes/{usuarioId}
    @GetMapping("/{usuarioId}")
    public ResponseEntity<List<Reporte>> listarReportes(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(reporteService.obtenerReportesPorUsuario(usuarioId));
    }

    // DELETE /api/reportes/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarReporte(@PathVariable Long id) {
        try {
            reporteService.eliminarReporte(id);
            return ResponseEntity.ok(Map.of("mensaje", "Reporte eliminado correctamente."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
