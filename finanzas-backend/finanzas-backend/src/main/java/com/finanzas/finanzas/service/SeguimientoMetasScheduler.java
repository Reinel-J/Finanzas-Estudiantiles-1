package com.finanzas.finanzas.service;

import com.finanzas.finanzas.dto.ProductoMLDTO;
import com.finanzas.finanzas.model.HistorialPrecio;
import com.finanzas.finanzas.model.MetaAhorro;
import com.finanzas.finanzas.model.Notificacion;
import com.finanzas.finanzas.repository.HistorialPrecioRepository;
import com.finanzas.finanzas.repository.MetaAhorroRepository;
import com.finanzas.finanzas.repository.NotificacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SeguimientoMetasScheduler {

    @Autowired
    private MetaAhorroRepository metaAhorroRepository;

    @Autowired
    private HistorialPrecioRepository historialPrecioRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private MercadoLibreService mercadoLibreService;

    /**
     * Revisa los precios de los productos en seguimiento.
     * Se ejecuta cada 6 horas: 0 0 0/6 * * *
     * Por facilidad de pruebas, usaremos cada minuto en modo dev: 0 * * * * *
     * o bien el fixedRate de 6 horas = 21600000 ms.
     * Dejaremos fixedRate de 6h para el requerimiento: 21600000
     */
    @Scheduled(fixedRate = 21600000, initialDelay = 10000) // 10 seg inicial, luego c/6h
    public void revisarPreciosMetasAhorro() {
        System.out.println("Iniciando revisión automática de precios de metas de ahorro...");

        // Buscar metas en progreso con seguimiento activo y external_id
        List<MetaAhorro> metasActivas = metaAhorroRepository.findAll().stream()
                .filter(m -> "EN_PROGRESO".equals(m.getEstado()) &&
                        Boolean.TRUE.equals(m.getSeguimientoActivo()) &&
                        m.getExternalProductId() != null &&
                        !m.getExternalProductId().isEmpty())
                .toList();

        for (MetaAhorro meta : metasActivas) {
            try {
                // Prevenir revisiones múltiples muy seguidas (ej: evitar spamear a ML)
                if (meta.getUltimaRevision() != null && 
                    meta.getUltimaRevision().isAfter(LocalDateTime.now().minusHours(2))) {
                    continue; 
                }

                ProductoMLDTO productoActualizado = mercadoLibreService.obtenerProductoPorId(meta.getExternalProductId());

                if (productoActualizado != null) {
                    procesarActualizacionPrecio(meta, productoActualizado);
                }

            } catch (Exception e) {
                System.err.println("Error revisando precio de meta ID " + meta.getId() + ": " + e.getMessage());
            }
        }
        System.out.println("Revisión automática de precios completada.");
    }

    private void procesarActualizacionPrecio(MetaAhorro meta, ProductoMLDTO productoInfo) {
        Double precioAnterior = meta.getUltimoPrecioDetectado();
        Double nuevoPrecio = productoInfo.getPrecio();
        LocalDateTime ahora = LocalDateTime.now();

        // 1. Guardar en el historial de precios
        HistorialPrecio historial = new HistorialPrecio();
        historial.setMeta(meta);
        historial.setPrecio(nuevoPrecio);
        historial.setPrecioOriginal(productoInfo.getPrecioOriginal());
        historial.setDescuentoPorcentaje(productoInfo.getDescuentoPorcentaje());
        historial.setTituloProducto(productoInfo.getTitulo());
        historial.setImagenUrl(productoInfo.getImagenUrl());
        historial.setLinkProducto(productoInfo.getLink());
        historial.setFechaRevision(ahora);
        historial.setMarketplace("MERCADOLIBRE");
        historialPrecioRepository.save(historial);

        // 2. Actualizar la meta con los datos más recientes
        meta.setUltimaRevision(ahora);
        meta.setUltimoPrecioDetectado(nuevoPrecio);
        meta.setUltimoPrecioOriginal(productoInfo.getPrecioOriginal());
        meta.setUltimoDescuentoPorcentaje(productoInfo.getDescuentoPorcentaje());
        meta.setProductTitle(productoInfo.getTitulo());
        meta.setProductImageUrl(productoInfo.getImagenUrl());
        
        // Actualizar mejor precio histórico
        if (meta.getMejorPrecioHistorico() == null || nuevoPrecio < meta.getMejorPrecioHistorico()) {
            meta.setMejorPrecioHistorico(nuevoPrecio);
        }

        metaAhorroRepository.save(meta);

        // 3. Generar Notificaciones si aplica
        generarNotificacionesInteligentes(meta, precioAnterior, nuevoPrecio, productoInfo);
    }

    private void generarNotificacionesInteligentes(MetaAhorro meta, Double precioAnterior, Double nuevoPrecio, ProductoMLDTO productoInfo) {
        
        if (precioAnterior == null) return; // Primera vez que se revisa, no hay con qué comparar

        // Caso A: Descuento explícito detectado por ML
        if (productoInfo.getDescuentoPorcentaje() != null && productoInfo.getDescuentoPorcentaje() > 5.0) {
             crearNotificacion(meta,
                     Notificacion.TipoNotificacion.DESCUENTO,
                     "¡Descuento en " + meta.getNombre() + "!",
                     "El producto " + productoInfo.getTitulo() + " tiene un " + productoInfo.getDescuentoPorcentaje() + "% de descuento. ¡Aprovecha el nuevo precio de $" + nuevoPrecio + "!"
             );
        }

        // Caso B: Alcanzó o bajó del precio objetivo del usuario
        if (meta.getPrecioObjetivoCompra() != null && nuevoPrecio <= meta.getPrecioObjetivoCompra() && precioAnterior > meta.getPrecioObjetivoCompra()) {
             crearNotificacion(meta,
                     Notificacion.TipoNotificacion.PRECIO_BAJO,
                     "¡Precio Objetivo Alcanzado!",
                     "El producto " + productoInfo.getTitulo() + " bajó a $" + nuevoPrecio + ", que es igual o menor a tu precio esperado de $" + meta.getPrecioObjetivoCompra() + "."
             );
        } 
        // Caso C: Bajada de precio significativa (más del 5% respecto al precio anterior)
        else if (nuevoPrecio < precioAnterior * 0.95) {
             crearNotificacion(meta,
                     Notificacion.TipoNotificacion.PRECIO_BAJO,
                     "¡Bajada de precio en tu Meta!",
                     "El precio de " + productoInfo.getTitulo() + " bajó de $" + precioAnterior + " a $" + nuevoPrecio + "."
             );
        }

        // Caso D: Ahorro suficiente para comprar ya
        if (meta.getMontoActual() >= nuevoPrecio && meta.getMontoActual() < precioAnterior) {
            crearNotificacion(meta,
                    Notificacion.TipoNotificacion.META_COMPLETADA,
                    "¡Ya puedes comprarlo!",
                    "Con tu ahorro actual de $" + meta.getMontoActual() + " y el nuevo precio de $" + nuevoPrecio + ", ¡ya te alcanza para comprar " + productoInfo.getTitulo() + "!"
            );
        }
    }

    private void crearNotificacion(MetaAhorro meta, Notificacion.TipoNotificacion tipo, String titulo, String mensaje) {
        Notificacion notif = new Notificacion();
        notif.setUsuario(meta.getUsuario());
        notif.setMetaId(meta.getId());
        notif.setTipo(tipo);
        notif.setTitulo(titulo);
        notif.setMensaje(mensaje);
        notif.setFecha(LocalDateTime.now());
        notif.setLeida(false);
        notificacionRepository.save(notif);
    }
}
