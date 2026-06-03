package com.finanzas.finanzas.controller;

import com.finanzas.finanzas.service.MonedaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/moneda")
@CrossOrigin(origins = "*")
public class MonedaController {

    @Autowired
    private MonedaService monedaService;

    @PostMapping("/convertir")
    public ResponseEntity<?> convertir(@RequestBody Map<String, Object> datos) {
        try {
            double monto = ((Number) datos.get("monto")).doubleValue();
            String monedaOrigen = (String) datos.get("monedaOrigen");
            String monedaDestino = (String) datos.get("monedaDestino");

            if (monto <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "El monto debe ser mayor a 0."));
            }
            if (monedaOrigen == null || monedaDestino == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Debes especificar monedaOrigen y monedaDestino."));
            }

            Map<String, Object> resultado = monedaService.convertir(monto, monedaOrigen, monedaDestino);
            return ResponseEntity.ok(resultado);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Solicitud inválida."));
        }
    }
}