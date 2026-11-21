package com.finanzas.finanzas.service;

import com.finanzas.finanzas.model.Gasto;
import com.finanzas.finanzas.model.Usuario;
import com.finanzas.finanzas.repository.GastoRepository;
import com.finanzas.finanzas.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalisisIntegralService {

    @Autowired
    private GastoRepository gastoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    /**
     * Calcula el valor promedio diario de gasto usando la Regla del Trapecio
     * (Cálculo Integral).
     *
     * Formula: (1/(b-a)) * Integral[a,b] f(x) dx
     * donde f(x) = gasto acumulado por dia.
     *
     * La integral se aproxima con la Regla del Trapecio:
     *   h/2 * [f(x0) + 2*f(x1) + 2*f(x2) + ... + 2*f(xn-1) + f(xn)]
     */
    public Map<String, Object> calcularPromedioDiarioIntegral(Long usuarioId, LocalDate fechaInicio, LocalDate fechaFin) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));

        if (fechaFin.isBefore(fechaInicio)) {
            throw new IllegalArgumentException("fechaFin no puede ser anterior a fechaInicio.");
        }

        List<Gasto> gastos = gastoRepository.findGastosSemanales(usuario, fechaInicio, fechaFin);

        int dias = (int) java.time.temporal.ChronoUnit.DAYS.between(fechaInicio, fechaFin);
        if (dias <= 0) {
            dias = 1;
        }

        // Agrupar gastos por fecha
        Map<LocalDate, Double> gastoPorDia = new LinkedHashMap<>();
        for (int i = 0; i <= dias; i++) {
            gastoPorDia.put(fechaInicio.plusDays(i), 0.0);
        }
        for (Gasto g : gastos) {
            gastoPorDia.merge(g.getFecha(), g.getMonto(), Double::sum);
        }

        // Aplicar Regla del Trapecio
        List<Double> valores = new ArrayList<>(gastoPorDia.values());
        double h = 1.0; // paso = 1 dia
        double integral = reglaTrapecio(valores, h);

        // Valor promedio = (1/(b-a)) * integral
        double valorPromedio = integral / dias;

        // Para comparacion: promedio simple (sin integral)
        double sumaTotal = gastos.stream().mapToDouble(Gasto::getMonto).sum();
        double promedioSimple = sumaTotal / dias;

        return Map.of(
                "fechaInicio", fechaInicio.toString(),
                "fechaFin", fechaFin.toString(),
                "dias", dias,
                "integralGasto", Math.round(integral * 100.0) / 100.0,
                "valorPromedioDiario", Math.round(valorPromedio * 100.0) / 100.0,
                "promedioSimple", Math.round(promedioSimple * 100.0) / 100.0,
                "metodo", "Regla del Trapecio (Calculo Integral)",
                "gastosPorDia", gastoPorDia
        );
    }

    /**
     * Regla del Trapecio compuesta para aproximar una integral definida.
     *
     * @param valores array de f(x0), f(x1), ..., f(xn)
     * @param h       paso uniforme entre puntos consecutivos
     * @return aproximacion de la integral
     */
    private double reglaTrapecio(List<Double> valores, double h) {
        int n = valores.size() - 1;
        if (n < 1) return 0.0;

        double suma = valores.get(0) + valores.get(n);
        for (int i = 1; i < n; i++) {
            suma += 2 * valores.get(i);
        }
        return (h / 2.0) * suma;
    }
}
