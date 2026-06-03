package com.finanzas.finanzas.service;

import com.finanzas.finanzas.model.Reporte;
import com.finanzas.finanzas.model.Usuario;
import com.finanzas.finanzas.repository.ReporteRepository;
import com.finanzas.finanzas.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReporteService {

    @Autowired
    private ReporteRepository reporteRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    public Reporte guardarReporte(Long usuarioId, Reporte reporte) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + usuarioId));
        
        reporte.setUsuario(usuario);
        return reporteRepository.save(reporte);
    }

    public List<Reporte> obtenerReportesPorUsuario(Long usuarioId) {
        return reporteRepository.findByUsuarioIdOrderByFechaGeneracionDesc(usuarioId);
    }

    public void eliminarReporte(Long id) {
        if (!reporteRepository.existsById(id)) {
            throw new RuntimeException("El reporte con ID " + id + " no existe.");
        }
        reporteRepository.deleteById(id);
    }
}
