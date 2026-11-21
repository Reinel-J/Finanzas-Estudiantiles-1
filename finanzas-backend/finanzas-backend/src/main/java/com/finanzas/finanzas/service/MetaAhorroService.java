package com.finanzas.finanzas.service;

import com.finanzas.finanzas.model.MetaAhorro;
import com.finanzas.finanzas.model.Usuario;
import com.finanzas.finanzas.repository.MetaAhorroRepository;
import com.finanzas.finanzas.repository.UsuarioRepository;
import com.finanzas.finanzas.model.Gasto;
import com.finanzas.finanzas.service.GastoService;
import com.finanzas.finanzas.service.IngresoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class MetaAhorroService {

    @Autowired
    private MetaAhorroRepository metaAhorroRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private GastoService gastoService;

    @Autowired
    private IngresoService ingresoService;

    // ── Crear meta ────────────────────────────────────────────
    public MetaAhorro crear(Long usuarioId, MetaAhorro meta) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
        if (meta.getNombre() == null || meta.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre de la meta es obligatorio.");
        }
        if (meta.getMontoObjetivo() == null || meta.getMontoObjetivo() <= 0) {
            throw new IllegalArgumentException("El monto objetivo debe ser mayor a 0.");
        }
        // Ignorar un id enviado por el cliente: si no, save() sobrescribiría otra meta
        meta.setId(null);
        meta.setUsuario(usuario);
        meta.setMontoActual(0.0);
        meta.setEstado("EN_PROGRESO");
        return metaAhorroRepository.save(meta);
    }

    // ── Listar metas de un usuario ────────────────────────────
    public List<MetaAhorro> listar(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
        return metaAhorroRepository.findByUsuario(usuario);
    }

    // ── Abonar a una meta ─────────────────────────────────────
    @Transactional
    public MetaAhorro abonar(Long metaId, Double monto) {
        if (monto == null || monto <= 0) {
            throw new IllegalArgumentException("El monto a abonar debe ser mayor a 0.");
        }
        MetaAhorro meta = metaAhorroRepository.findById(metaId)
                .orElseThrow(() -> new RuntimeException("Meta no encontrada."));

        if (!"EN_PROGRESO".equals(meta.getEstado())) {
            throw new IllegalStateException("Solo se puede abonar a metas en progreso.");
        }

        // No cobrar más de lo que falta: antes, abonar de más registraba el gasto
        // completo pero la meta se topaba en el objetivo y el excedente se perdía.
        double actual = meta.getMontoActual() != null ? meta.getMontoActual() : 0.0;
        double restante = meta.getMontoObjetivo() - actual;
        if (restante <= 0) {
            throw new IllegalStateException("La meta ya alcanzó su monto objetivo.");
        }
        monto = Math.min(monto, restante);

        Long usuarioId = meta.getUsuario().getId();

        // 1. Verificar si hay saldo suficiente
        Double totalI = ingresoService.totalIngresos(usuarioId);
        if (totalI == null) totalI = 0.0;
        Double totalG = gastoService.totalGastos(usuarioId);
        if (totalG == null) totalG = 0.0;

        Double saldoActual = totalI - totalG;

        if (monto > saldoActual) {
            throw new IllegalStateException("Saldo insuficiente. Tu saldo actual es de $"
                    + String.format(new java.util.Locale("es", "CO"), "%,.0f", Math.max(saldoActual, 0)));
        }

        // 2. Registrar el abono como un gasto para que se descuente del saldo
        Gasto abonoGasto = new Gasto();
        abonoGasto.setMonto(monto);
        abonoGasto.setDescripcion("Abono a meta: " + meta.getNombre());
        abonoGasto.setFecha(LocalDate.now());
        abonoGasto.setCategoria("Ahorro");
        gastoService.registrarSimple(usuarioId, abonoGasto);

        // 3. Actualizar el monto de la meta
        double nuevoMonto = actual + monto;
        meta.setMontoActual(nuevoMonto);

        // Si alcanzó el objetivo → completada
        if (nuevoMonto >= meta.getMontoObjetivo()) {
            meta.setEstado("COMPLETADA");
        }
        return metaAhorroRepository.save(meta);
    }

    // ── Calcular porcentaje de progreso ───────────────────────
    public double calcularProgreso(MetaAhorro meta) {
        if (meta.getMontoObjetivo() == null || meta.getMontoObjetivo() == 0 || meta.getMontoActual() == null)
            return 0;
        return (meta.getMontoActual() / meta.getMontoObjetivo()) * 100;
    }

    // ── Editar meta ───────────────────────────────────────────
    public MetaAhorro editar(Long id, MetaAhorro datosNuevos) {
        MetaAhorro meta = metaAhorroRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Meta no encontrada."));
        if (datosNuevos.getMontoObjetivo() == null || datosNuevos.getMontoObjetivo() <= 0) {
            throw new IllegalArgumentException("El monto objetivo debe ser mayor a 0.");
        }
        meta.setNombre(datosNuevos.getNombre());
        meta.setMontoObjetivo(datosNuevos.getMontoObjetivo());
        meta.setFechaLimite(datosNuevos.getFechaLimite());
        return metaAhorroRepository.save(meta);
    }

    // ── Cancelar meta ─────────────────────────────────────────
    public MetaAhorro cancelar(Long id) {
        MetaAhorro meta = metaAhorroRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Meta no encontrada."));
        meta.setEstado("CANCELADA");
        return metaAhorroRepository.save(meta);
    }

    // ── Eliminar meta ─────────────────────────────────────────
    public void eliminar(Long id) {
        if (!metaAhorroRepository.existsById(id)) {
            throw new RuntimeException("Meta no encontrada.");
        }
        metaAhorroRepository.deleteById(id);
    }

    // ── Verificar Descuentos (DummyJSON API) ──────────────────
    public List<Map<String, Object>> verificarDescuentos(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
        
        List<MetaAhorro> metas = metaAhorroRepository.findByUsuario(usuario);
        java.util.List<Map<String, Object>> ofertas = new java.util.ArrayList<>();
        org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();

        for (MetaAhorro meta : metas) {
            if (meta.getIdProductoExterno() != null && !meta.getIdProductoExterno().trim().isEmpty() && !"COMPLETADA".equals(meta.getEstado())) {
                try {
                    String url = "https://dummyjson.com/products/" + meta.getIdProductoExterno().trim();
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> response = restTemplate.getForObject(url, java.util.Map.class);
                    if (response != null && response.get("discountPercentage") != null) {
                        double discount = ((Number) response.get("discountPercentage")).doubleValue();
                        if (discount > 0) {
                            double price = ((Number) response.get("price")).doubleValue();
                            String title = (String) response.get("title");
                            ofertas.add(java.util.Map.of(
                                "metaId", meta.getId(),
                                "metaNombre", meta.getNombre(),
                                "producto", title,
                                "precioActual", price,
                                "descuento", discount
                            ));
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error verificando descuento para meta " + meta.getId() + ": " + e.getMessage());
                }
            }
        }
        return ofertas;
    }
}
