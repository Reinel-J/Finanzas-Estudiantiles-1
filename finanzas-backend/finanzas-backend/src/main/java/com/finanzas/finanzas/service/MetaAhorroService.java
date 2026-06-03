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
    public MetaAhorro abonar(Long metaId, Double monto) {
        MetaAhorro meta = metaAhorroRepository.findById(metaId)
                .orElseThrow(() -> new RuntimeException("Meta no encontrada."));

        Long usuarioId = meta.getUsuario().getId();

        // 1. Verificar si hay saldo suficiente
        Double totalI = ingresoService.totalIngresos(usuarioId);
        if (totalI == null) totalI = 0.0;
        Double totalG = gastoService.totalGastos(usuarioId);
        if (totalG == null) totalG = 0.0;

        Double saldoActual = totalI - totalG;

        if (monto > saldoActual) {
            throw new RuntimeException("Saldo insuficiente. Tu saldo actual es de $" + saldoActual);
        }

        // 2. Registrar el abono como un gasto para que se descuente del saldo
        Gasto abonoGasto = new Gasto();
        abonoGasto.setMonto(monto);
        abonoGasto.setDescripcion("Abono a meta: " + meta.getNombre());
        abonoGasto.setFecha(LocalDate.now());
        abonoGasto.setCategoria("Ahorro");
        gastoService.registrarSimple(usuarioId, abonoGasto);

        // 3. Actualizar el monto de la meta
        double nuevoMonto = meta.getMontoActual() + monto;
        meta.setMontoActual(nuevoMonto);

        // Si alcanzó o superó el objetivo → completada
        if (nuevoMonto >= meta.getMontoObjetivo()) {
            meta.setMontoActual(meta.getMontoObjetivo());
            meta.setEstado("COMPLETADA");
        }
        return metaAhorroRepository.save(meta);
    }

    // ── Calcular porcentaje de progreso ───────────────────────
    public double calcularProgreso(MetaAhorro meta) {
        if (meta.getMontoObjetivo() == 0)
            return 0;
        return (meta.getMontoActual() / meta.getMontoObjetivo()) * 100;
    }

    // ── Editar meta ───────────────────────────────────────────
    public MetaAhorro editar(Long id, MetaAhorro datosNuevos) {
        MetaAhorro meta = metaAhorroRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Meta no encontrada."));
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
            if (meta.getIdProductoExterno() != null && !meta.getIdProductoExterno().trim().isEmpty() && !meta.getEstado().equals("COMPLETADA")) {
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
