package com.finanzas.finanzas.service;

import com.finanzas.finanzas.model.Ingreso;
import com.finanzas.finanzas.model.Usuario;
import com.finanzas.finanzas.repository.IngresoRepository;
import com.finanzas.finanzas.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class IngresoService {

    @Autowired
    private IngresoRepository ingresoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private GastoService gastoService;

    // ── Agregar ingreso (asigna semestre automático) ───────────
    @Transactional
    public Ingreso agregar(Long usuarioId, Ingreso ingreso) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));

        // Asigna semestre del usuario si no se envió
        if (ingreso.getSemestre() == null) {
            ingreso.setSemestre(usuario.getSemestreActual());
        }

        // Guardar ingreso con el monto COMPLETO original
        ingreso.setUsuario(usuario);
        Ingreso ingresoGuardado = ingresoRepository.save(ingreso);

        // Si tiene porcentaje de ahorro configurado, separar ahorro explícitamente
        if (usuario.getPorcentajeAhorro() != null && usuario.getPorcentajeAhorro() > 0) {
            double montoAhorro = ingresoGuardado.getMonto() * (usuario.getPorcentajeAhorro() / 100.0);

            // Acumular ahorro en el usuario
            double nuevoAhorro = usuario.getMontoAhorrado() + montoAhorro;
            usuario.setMontoAhorrado(nuevoAhorro);
            usuarioRepository.save(usuario);

            // CREAR UN GASTO EXPLÍCITO POR EL AHORRO AUTOMÁTICO
            // Esto asegura que el dinero se descuente del saldo y quede reflejado en el
            // historial.
            com.finanzas.finanzas.model.Gasto ahorroGasto = new com.finanzas.finanzas.model.Gasto();
            ahorroGasto.setMonto(montoAhorro);
            ahorroGasto.setDescripcion("Ahorro automático (" + usuario.getPorcentajeAhorro() + "%) de: "
                    + ingresoGuardado.getDescripcion());
            ahorroGasto.setFecha(ingresoGuardado.getFecha());
            ahorroGasto.setCategoria("Ahorro");
            gastoService.registrarSimple(usuarioId, ahorroGasto);
        }

        return ingresoGuardado;
    }

    // ── Listar ingresos de un usuario ─────────────────────────
    public List<Ingreso> listar(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
        return ingresoRepository.findByUsuarioOrderByFechaDesc(usuario);
    }

    // ── Listar por semestre ─────────────────────────────────────
    public List<Ingreso> listarPorSemestre(Long usuarioId, Integer semestre) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
        return ingresoRepository.findByUsuarioAndSemestreOrderByFechaDesc(usuario, semestre);
    }

    // ── Ingresos del mes actual ────────────────────────────────
    public List<Ingreso> listarMesActual(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
        return ingresoRepository.findIngresosMesActual(usuario);
    }

    // ── Ingresos semanales ─────────────────────────────────────
    public List<Ingreso> listarSemanales(Long usuarioId, LocalDate fechaInicio, LocalDate fechaFin) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
        return ingresoRepository.findIngresosSemanales(usuario, fechaInicio, fechaFin);
    }

    // ── Total de ingresos ──────────────────────────────────────
    public Double totalIngresos(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
        return ingresoRepository.sumMontoByUsuario(usuario);
    }

    // ── Total por semestre ─────────────────────────────────────
    public Double totalPorSemestre(Long usuarioId, Integer semestre) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
        return ingresoRepository.sumMontoByUsuarioAndSemestre(usuario, semestre);
    }

    // ── Editar ingreso ─────────────────────────────────────────
    public Ingreso editar(Long id, Ingreso datosNuevos) {
        Ingreso ingreso = ingresoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ingreso no encontrado."));
        ingreso.setMonto(datosNuevos.getMonto());
        ingreso.setDescripcion(datosNuevos.getDescripcion());
        ingreso.setFecha(datosNuevos.getFecha());
        ingreso.setCategoria(datosNuevos.getCategoria());
        ingreso.setSemestre(datosNuevos.getSemestre());
        return ingresoRepository.save(ingreso);
    }

    // ── Eliminar ingreso ───────────────────────────────────────
    public void eliminar(Long id) {
        if (!ingresoRepository.existsById(id)) {
            throw new RuntimeException("Ingreso no encontrado.");
        }
        ingresoRepository.deleteById(id);
    }
}