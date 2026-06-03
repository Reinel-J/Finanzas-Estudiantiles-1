package com.finanzas.finanzas.service;

import com.finanzas.finanzas.model.HistorialSemestre;
import com.finanzas.finanzas.model.Usuario;
import com.finanzas.finanzas.repository.GastoRepository;
import com.finanzas.finanzas.repository.HistorialSemestreRepository;
import com.finanzas.finanzas.repository.IngresoRepository;
import com.finanzas.finanzas.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private IngresoRepository ingresoRepository;

    @Autowired
    private GastoRepository gastoRepository;

    @Autowired
    private HistorialSemestreRepository historialSemestreRepository;

    // ── Registro ──────────────────────────────────────────────
    public Usuario registrar(Usuario usuario) {
        if (usuarioRepository.existsByNombre(usuario.getNombre())) {
            throw new RuntimeException("El nombre de usuario ya existe.");
        }
        if (usuarioRepository.existsBynDoc(usuario.getNDoc())) {
            throw new RuntimeException("El número de documento ya está registrado.");
        }
        // Valores por defecto para semestre y ahorro
        if (usuario.getSemestreActual() == null) {
            usuario.setSemestreActual(1);
        }
        if (usuario.getPorcentajeAhorro() == null) {
            usuario.setPorcentajeAhorro(10.0);
        }
        if (usuario.getMontoAhorrado() == null) {
            usuario.setMontoAhorrado(0.0);
        }
        return usuarioRepository.save(usuario);
    }

    // ── Login ─────────────────────────────────────────────────
    public Optional<Usuario> login(String nombre, String contrasena) {
        return usuarioRepository.findByNombre(nombre)
                .filter(u -> u.getContrasena().equals(contrasena));
    }

    // ── Buscar por ID ─────────────────────────────────────────
    public Optional<Usuario> buscarPorId(Long id) {
        return usuarioRepository.findById(id);
    }

    // ── Listar todos ──────────────────────────────────────────
    public java.util.List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    // ── Actualizar perfil ─────────────────────────────────────
    public Usuario actualizar(Long id, Usuario datosNuevos) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));

        usuario.setApellido(datosNuevos.getApellido());
        usuario.setUniversidad(datosNuevos.getUniversidad());
        // Solo actualizar contraseña si se envía una nueva
        if (datosNuevos.getContrasena() != null && !datosNuevos.getContrasena().isBlank()) {
            usuario.setContrasena(datosNuevos.getContrasena());
        }
        return usuarioRepository.save(usuario);
    }

    // ── Configurar semestre ──────────────────────────────────
    public Usuario configurarSemestre(Long id, Integer semestre, String fechaInicioStr,
                                       String fechaFinStr, Double porcentajeAhorro) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));

        if (semestre != null) {
            usuario.setSemestreActual(semestre);
        }
        if (fechaInicioStr != null && !fechaInicioStr.isBlank()) {
            try {
                usuario.setFechaInicioSemestre(LocalDate.parse(fechaInicioStr));
            } catch (DateTimeParseException e) {
                throw new RuntimeException("Formato de fecha inicio inválido. Use YYYY-MM-DD.");
            }
        }
        if (fechaFinStr != null && !fechaFinStr.isBlank()) {
            try {
                usuario.setFechaFinSemestre(LocalDate.parse(fechaFinStr));
            } catch (DateTimeParseException e) {
                throw new RuntimeException("Formato de fecha fin inválido. Use YYYY-MM-DD.");
            }
        }
        if (porcentajeAhorro != null && porcentajeAhorro >= 0 && porcentajeAhorro <= 100) {
            usuario.setPorcentajeAhorro(porcentajeAhorro);
        }

        return usuarioRepository.save(usuario);
    }

    // ── Agregar ahorro ────────────────────────────────────────
    public Usuario agregarAhorro(Long id, Double monto) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
        double nuevo = usuario.getMontoAhorrado() + monto;
        usuario.setMontoAhorrado(nuevo);
        return usuarioRepository.save(usuario);
    }

    // ── Retirar ahorro ────────────────────────────────────────
    public Usuario retirarAhorro(Long id, Double monto) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
        if (monto > usuario.getMontoAhorrado()) {
            throw new RuntimeException("No tienes suficiente ahorro para retirar esa cantidad.");
        }
        double nuevo = usuario.getMontoAhorrado() - monto;
        usuario.setMontoAhorrado(nuevo);
        return usuarioRepository.save(usuario);
    }

    // ── Finalizar semestre ────────────────────────────────────
    @Transactional
    public Map<String, Object> finalizarSemestre(Long id, int asignaturasPerdidas) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));

        if (usuario.getFechaFinSemestre() == null) {
            throw new RuntimeException("No se ha configurado la fecha de fin del semestre.");
        }

        LocalDate hoy = LocalDate.now();
        if (hoy.isBefore(usuario.getFechaFinSemestre())) {
            throw new RuntimeException("El semestre aún no ha finalizado. Fecha de fin: " + usuario.getFechaFinSemestre());
        }

        Integer semestreActual = usuario.getSemestreActual();
        LocalDate fechaInicio = usuario.getFechaInicioSemestre();
        LocalDate fechaFin = usuario.getFechaFinSemestre();

        Double totalIngresos = ingresoRepository.sumMontoByUsuarioAndSemestre(usuario, semestreActual);
        if (totalIngresos == null) totalIngresos = 0.0;

        Double totalGastos = gastoRepository.sumMontoByUsuarioAndSemestre(usuario, semestreActual);
        if (totalGastos == null) totalGastos = 0.0;

        Double saldoFinal = totalIngresos - totalGastos;
        Double montoAhorradoFinal = usuario.getMontoAhorrado();

        Long cantidadIngresos = ingresoRepository.findByUsuarioAndSemestreOrderByFechaDesc(usuario, semestreActual).stream().count();
        Long cantidadGastos = gastoRepository.findByUsuarioAndSemestreOrderByFechaDesc(usuario, semestreActual).stream().count();

        List<Object[]> gastosPorCategoriaRaw = gastoRepository.sumGastosPorCategoria(usuario);
        Map<String, Double> gastosPorCategoria = gastosPorCategoriaRaw.stream()
                .collect(Collectors.toMap(
                        fila -> fila[0] != null ? fila[0].toString() : "Sin categoría",
                        fila -> ((Number) fila[1]).doubleValue()
                ));

        HistorialSemestre historial = new HistorialSemestre();
        historial.setUsuario(usuario);
        historial.setSemestreNumero(semestreActual);
        historial.setFechaInicio(fechaInicio);
        historial.setFechaFin(fechaFin);
        historial.setTotalIngresos(totalIngresos);
        historial.setTotalGastos(totalGastos);
        historial.setSaldoFinal(saldoFinal);
        historial.setMontoAhorradoFinal(montoAhorradoFinal);
        historial.setCantidadIngresos(cantidadIngresos.intValue());
        historial.setCantidadGastos(cantidadGastos.intValue());
        historial.setGastosPorCategoria(gastosPorCategoria.toString());
        historialSemestreRepository.save(historial);

        ingresoRepository.deleteAll(ingresoRepository.findByUsuarioAndSemestreOrderByFechaDesc(usuario, semestreActual));
        gastoRepository.deleteAll(gastoRepository.findByUsuarioAndSemestreOrderByFechaDesc(usuario, semestreActual));

        String mensajeAvance = "";
        if (asignaturasPerdidas > 3) {
            usuario.setSemestreActual(semestreActual);
            mensajeAvance = "Te mantienes en el semestre " + semestreActual + " por haber perdido más de 3 asignaturas.";
        } else {
            usuario.setSemestreActual(semestreActual + 1);
            mensajeAvance = "¡Felicidades! Avanzas al semestre " + (semestreActual + 1) + ".";
        }

        usuario.setFechaInicioSemestre(null);
        usuario.setFechaFinSemestre(null);
        usuario.setMontoAhorrado(0.0);
        usuarioRepository.save(usuario);

        return Map.of(
                "mensaje", "Semestre " + semestreActual + " finalizado correctamente.",
                "mensajeAvance", mensajeAvance,
                "semestreNumero", semestreActual,
                "totalIngresos", totalIngresos,
                "totalGastos", totalGastos,
                "saldoFinal", saldoFinal,
                "montoAhorradoFinal", montoAhorradoFinal,
                "gastosPorCategoria", gastosPorCategoria
        );
    }

    // ── Obtener historial de semestres ────────────────────────
    public List<HistorialSemestre> obtenerHistorial(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
        return historialSemestreRepository.findByUsuarioIdOrderBySemestreNumeroDesc(usuario.getId());
    }
}