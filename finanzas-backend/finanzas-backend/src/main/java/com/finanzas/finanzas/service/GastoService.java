package com.finanzas.finanzas.service;

import com.finanzas.finanzas.model.Gasto;
import com.finanzas.finanzas.model.Ingreso;
import com.finanzas.finanzas.model.Usuario;
import com.finanzas.finanzas.repository.GastoRepository;
import com.finanzas.finanzas.repository.IngresoRepository;
import com.finanzas.finanzas.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GastoService {

    @Autowired
    private GastoRepository gastoRepository;

    @Autowired
    private IngresoRepository ingresoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private static void validar(Gasto gasto) {
        if (gasto.getMonto() == null || gasto.getMonto() <= 0) {
            throw new IllegalArgumentException("El monto del gasto debe ser mayor a 0.");
        }
        if (gasto.getFecha() == null) {
            throw new IllegalArgumentException("La fecha del gasto es obligatoria (YYYY-MM-DD).");
        }
    }

    // ── Registrar gasto asociado a un ingreso ──────────────────
    // Antes se restaba el gasto del monto del ingreso Y además se contaba como
    // gasto, así que el saldo (ingresos - gastos) bajaba el doble. Ahora el
    // ingreso no se modifica; solo se valida que le quede disponible.
    public Gasto registrar(Long usuarioId, Long ingresoId, Gasto gasto) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
        validar(gasto);

        Ingreso ingreso = ingresoRepository.findById(ingresoId)
                .orElseThrow(() -> new RuntimeException("Ingreso no encontrado."));
        if (!ingreso.getUsuario().getId().equals(usuario.getId())) {
            throw new IllegalArgumentException("El ingreso no pertenece a este usuario.");
        }

        double disponible = ingreso.getMonto() - gastoRepository.sumMontoByIngreso(ingreso);
        if (gasto.getMonto() > disponible) {
            throw new IllegalStateException("El gasto supera el monto disponible del ingreso ($"
                    + String.format(new java.util.Locale("es", "CO"), "%,.0f", Math.max(disponible, 0)) + ").");
        }

        if (gasto.getSemestre() == null) {
            gasto.setSemestre(usuario.getSemestreActual());
        }

        gasto.setId(null);
        gasto.setUsuario(usuario);
        gasto.setIngreso(ingreso);
        return gastoRepository.save(gasto);
    }

    // ── Registrar gasto simple (sin asociar a ingreso) ────────
    public Gasto registrarSimple(Long usuarioId, Gasto gasto) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
        validar(gasto);
        // Ignorar un id enviado por el cliente: si no, save() sobrescribiría otro gasto
        gasto.setId(null);
        gasto.setUsuario(usuario);

        if (gasto.getSemestre() == null) {
            gasto.setSemestre(usuario.getSemestreActual());
        }

        return gastoRepository.save(gasto);
    }

    // ── Listar gastos de un usuario ───────────────────────────
    public List<Gasto> listar(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
        return gastoRepository.findByUsuarioOrderByFechaDesc(usuario);
    }

    // ── Listar por semestre ───────────────────────────────────
    public List<Gasto> listarPorSemestre(Long usuarioId, Integer semestre) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
        return gastoRepository.findByUsuarioAndSemestreOrderByFechaDesc(usuario, semestre);
    }

    // ── Gastos del mes actual ─────────────────────────────────
    public List<Gasto> listarMesActual(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
        return gastoRepository.findGastosMesActual(usuario);
    }

    // ── Gastos semanales ───────────────────────────────────────
    public List<Gasto> listarSemanales(Long usuarioId, LocalDate fechaInicio, LocalDate fechaFin) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
        return gastoRepository.findGastosSemanales(usuario, fechaInicio, fechaFin);
    }

    // ── Total de gastos ───────────────────────────────────────
    public Double totalGastos(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
        return gastoRepository.sumMontoByUsuario(usuario);
    }

    // ── Total por semestre ───────────────────────────────────
    public Double totalPorSemestre(Long usuarioId, Integer semestre) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
        return gastoRepository.sumMontoByUsuarioAndSemestre(usuario, semestre);
    }

    // ── Total semanal ─────────────────────────────────────────
    public Double totalSemanales(Long usuarioId, LocalDate fechaInicio, LocalDate fechaFin) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
        List<Gasto> gastos = gastoRepository.findGastosSemanales(usuario, fechaInicio, fechaFin);
        return gastos.stream().mapToDouble(Gasto::getMonto).sum();
    }

    // ── Gastos semanales por categoría ────────────────────────
    public Map<String, Double> gastosSemanalesPorCategoria(Long usuarioId, LocalDate fechaInicio, LocalDate fechaFin) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
        List<Object[]> resultado = gastoRepository.sumGastosSemanalesPorCategoria(usuario, fechaInicio, fechaFin);
        Map<String, Double> mapa = new HashMap<>();
        for (Object[] fila : resultado) {
            String categoria = fila[0] != null ? fila[0].toString() : "Sin categoría";
            Double total = ((Number) fila[1]).doubleValue();
            mapa.put(categoria, total);
        }
        return mapa;
    }

    // ── Gastos agrupados por categoría (para gráficas) ────────
    public Map<String, Double> gastosPorCategoria(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
        List<Object[]> resultado = gastoRepository.sumGastosPorCategoria(usuario);
        Map<String, Double> mapa = new HashMap<>();
        for (Object[] fila : resultado) {
            String categoria = fila[0] != null ? fila[0].toString() : "Sin categoría";
            Double total = ((Number) fila[1]).doubleValue();
            mapa.put(categoria, total);
        }
        return mapa;
    }

    // ── Resumen semanal (para notificaciones) ────────────────
    public Map<String, Object> resumenSemanal(Long usuarioId) {
        LocalDate hoy = LocalDate.now();
        // Últimos 7 días INCLUYENDO hoy. Con minusWeeks(1) y BETWEEN (inclusivo)
        // se tomaban 8 días y el total no cuadraba con la gráfica de 7 días.
        LocalDate fechaInicio = hoy.minusDays(6);

        List<Gasto> gastos = listarSemanales(usuarioId, fechaInicio, hoy);
        Double total = totalSemanales(usuarioId, fechaInicio, hoy);
        Map<String, Double> porCategoria = gastosSemanalesPorCategoria(usuarioId, fechaInicio, hoy);

        return Map.of(
                "fechaInicio", fechaInicio.toString(),
                "fechaFin", hoy.toString(),
                "totalGastado", total,
                "numeroGastos", gastos.size(),
                "gastos", gastos,
                "porCategoria", porCategoria
        );
    }

    // ── Editar gasto ──────────────────────────────────────────
    public Gasto editar(Long id, Gasto datosNuevos) {
        Gasto gasto = gastoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Gasto no encontrado."));
        validar(datosNuevos);
        gasto.setMonto(datosNuevos.getMonto());
        gasto.setDescripcion(datosNuevos.getDescripcion());
        gasto.setFecha(datosNuevos.getFecha());
        gasto.setCategoria(datosNuevos.getCategoria());
        if (datosNuevos.getSemestre() != null) {
            gasto.setSemestre(datosNuevos.getSemestre());
        }
        return gastoRepository.save(gasto);
    }

    // ── Eliminar gasto ──────────────────────────────────────
    // Si se borra un gasto de "Ahorro automático", ese dinero deja de estar
    // ahorrado: se descuenta también del monto ahorrado del usuario.
    @Transactional
    public void eliminar(Long id) {
        Gasto gasto = gastoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Gasto no encontrado."));
        if (gasto.getDescripcion() != null && gasto.getDescripcion().startsWith(IngresoService.PREFIJO_AHORRO_AUTO)) {
            Usuario usuario = gasto.getUsuario();
            usuario.setMontoAhorrado(Math.max(0.0, usuario.getMontoAhorrado() - gasto.getMonto()));
            usuarioRepository.save(usuario);
        }
        gastoRepository.delete(gasto);
    }
}