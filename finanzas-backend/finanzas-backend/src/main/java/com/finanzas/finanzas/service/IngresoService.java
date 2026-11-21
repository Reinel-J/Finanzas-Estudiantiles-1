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
import java.util.List;

@Service
public class IngresoService {

    @Autowired
    private IngresoRepository ingresoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private GastoService gastoService;

    @Autowired
    private GastoRepository gastoRepository;

    /** Prefijo de la descripción de los gastos que crea el ahorro automático */
    static final String PREFIJO_AHORRO_AUTO = "Ahorro automático";

    private static void validar(Ingreso ingreso) {
        if (ingreso.getMonto() == null || ingreso.getMonto() <= 0) {
            throw new IllegalArgumentException("El monto del ingreso debe ser mayor a 0.");
        }
        if (ingreso.getFecha() == null) {
            throw new IllegalArgumentException("La fecha del ingreso es obligatoria (YYYY-MM-DD).");
        }
    }

    // ── Agregar ingreso (asigna semestre automático) ───────────
    @Transactional
    public Ingreso agregar(Long usuarioId, Ingreso ingreso) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
        validar(ingreso);
        // Ignorar un id enviado por el cliente: si no, save() sobrescribiría otro ingreso
        ingreso.setId(null);

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
            Gasto ahorroGasto = new Gasto();
            ahorroGasto.setMonto(montoAhorro);
            ahorroGasto.setDescripcion(PREFIJO_AHORRO_AUTO + " (" + usuario.getPorcentajeAhorro() + "%) de: "
                    + ingresoGuardado.getDescripcion());
            // Se vincula al ingreso para poder revertirlo si el ingreso se elimina
            ahorroGasto.setIngreso(ingresoGuardado);
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
        validar(datosNuevos);
        ingreso.setMonto(datosNuevos.getMonto());
        ingreso.setDescripcion(datosNuevos.getDescripcion());
        ingreso.setFecha(datosNuevos.getFecha());
        ingreso.setCategoria(datosNuevos.getCategoria());
        if (datosNuevos.getSemestre() != null) {
            ingreso.setSemestre(datosNuevos.getSemestre());
        }
        return ingresoRepository.save(ingreso);
    }

    // ── Eliminar ingreso ───────────────────────────────────────
    // Revierte el ahorro automático generado por el ingreso. Antes, al borrar un
    // ingreso quedaban el gasto "Ahorro automático" y el monto ahorrado, de modo que
    // el saldo y el ahorro mostraban dinero que ya no existía.
    @Transactional
    public void eliminar(Long id) {
        Ingreso ingreso = ingresoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ingreso no encontrado."));

        double ahorroRevertido = 0.0;
        for (Gasto g : gastoRepository.findByIngreso(ingreso)) {
            boolean esAhorroAuto = g.getDescripcion() != null && g.getDescripcion().startsWith(PREFIJO_AHORRO_AUTO);
            if (esAhorroAuto) {
                ahorroRevertido += g.getMonto();
                gastoRepository.delete(g);
            } else {
                // Gasto normal asociado: se conserva pero se desvincula (evita error de FK)
                g.setIngreso(null);
                gastoRepository.save(g);
            }
        }
        if (ahorroRevertido > 0) {
            Usuario usuario = ingreso.getUsuario();
            usuario.setMontoAhorrado(Math.max(0.0, usuario.getMontoAhorrado() - ahorroRevertido));
            usuarioRepository.save(usuario);
        }
        ingresoRepository.delete(ingreso);
    }
}