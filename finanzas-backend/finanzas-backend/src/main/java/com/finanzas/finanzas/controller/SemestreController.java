package com.finanzas.finanzas.controller;

import com.finanzas.finanzas.model.HistorialSemestre;
import com.finanzas.finanzas.model.Usuario;
import com.finanzas.finanzas.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/semestre")
@CrossOrigin(origins = "*")
public class SemestreController {

    @Autowired
    private UsuarioService usuarioService;

    // PUT /api/semestre/{usuarioId}
    // Body: { semestreActual: 2, fechaInicioSemestre: "2026-01-15", fechaFinSemestre: "2026-06-30", porcentajeAhorro: 15 }
    @PutMapping("/{usuarioId}")
    public ResponseEntity<?> configurarSemestre(
            @PathVariable Long usuarioId,
            @RequestBody Map<String, Object> datos) {
        try {
            // Conversión segura: Jackson puede deserializar números JSON como Integer, Long, o Double
            Integer semestre = datos.get("semestreActual") != null
                    ? ((Number) datos.get("semestreActual")).intValue()
                    : null;
            String fechaInicioStr = datos.get("fechaInicioSemestre") != null
                    ? datos.get("fechaInicioSemestre").toString()
                    : null;
            String fechaFinStr = datos.get("fechaFinSemestre") != null
                    ? datos.get("fechaFinSemestre").toString()
                    : null;
            Double porcentaje = datos.get("porcentajeAhorro") != null
                    ? ((Number) datos.get("porcentajeAhorro")).doubleValue()
                    : null;

            Usuario usuario = usuarioService.configurarSemestre(
                    usuarioId, semestre, fechaInicioStr, fechaFinStr, porcentaje);

            return ResponseEntity.ok(usuario);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Datos inválidos: " + e.getMessage()));
        }
    }

    // GET /api/semestre/{usuarioId}
    @GetMapping("/{usuarioId}")
    public ResponseEntity<?> obtenerSemestre(@PathVariable Long usuarioId) {
        return usuarioService.buscarPorId(usuarioId)
                .map(u -> ResponseEntity.ok(Map.of(
                        "semestreActual", u.getSemestreActual() != null ? u.getSemestreActual() : 1,
                        "fechaInicioSemestre", u.getFechaInicioSemestre() != null ? u.getFechaInicioSemestre().toString() : "",
                        "fechaFinSemestre", u.getFechaFinSemestre() != null ? u.getFechaFinSemestre().toString() : "",
                        "porcentajeAhorro", u.getPorcentajeAhorro() != null ? u.getPorcentajeAhorro() : 10.0,
                        "montoAhorrado", u.getMontoAhorrado() != null ? u.getMontoAhorrado() : 0.0
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/semestre/{usuarioId}/ahorrar
    // Body: { monto: 50000 }
    @PostMapping("/{usuarioId}/ahorrar")
    public ResponseEntity<?> ahorrar(
            @PathVariable Long usuarioId,
            @RequestBody Map<String, Double> body) {
        try {
            Double monto = body.get("monto");
            if (monto == null || monto <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Monto inválido."));
            }
            Usuario usuario = usuarioService.agregarAhorro(usuarioId, monto);
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Ahorro registrado.",
                    "montoAhorrado", usuario.getMontoAhorrado()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // POST /api/semestre/{usuarioId}/retirar-ahorro
    // Body: { monto: 20000 }
    @PostMapping("/{usuarioId}/retirar-ahorro")
    public ResponseEntity<?> retirarAhorro(
            @PathVariable Long usuarioId,
            @RequestBody Map<String, Double> body) {
        try {
            Double monto = body.get("monto");
            if (monto == null || monto <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Monto inválido."));
            }
            Usuario usuario = usuarioService.retirarAhorro(usuarioId, monto);
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Retiro exitoso.",
                    "montoAhorrado", usuario.getMontoAhorrado()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // POST /api/semestre/{usuarioId}/finalizar
    @PostMapping("/{usuarioId}/finalizar")
    public ResponseEntity<?> finalizarSemestre(
            @PathVariable Long usuarioId,
            @RequestBody(required = false) Map<String, Object> body) {
        try {
            int perdidas = 0;
            if (body != null && body.get("asignaturasPerdidas") != null) {
                perdidas = ((Number) body.get("asignaturasPerdidas")).intValue();
            }
            Map<String, Object> resultado = usuarioService.finalizarSemestre(usuarioId, perdidas);
            return ResponseEntity.ok(resultado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/semestre/{usuarioId}/historial
    @GetMapping("/{usuarioId}/historial")
    public ResponseEntity<?> obtenerHistorial(@PathVariable Long usuarioId) {
        try {
            List<HistorialSemestre> historial = usuarioService.obtenerHistorial(usuarioId);
            return ResponseEntity.ok(historial);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}