package com.finanzas.finanzas.controller;

import com.finanzas.finanzas.dto.BusquedaProductoRequest;
import com.finanzas.finanzas.dto.ProductoMLDTO;
import com.finanzas.finanzas.dto.SeguimientoRequest;
import com.finanzas.finanzas.model.HistorialPrecio;
import com.finanzas.finanzas.model.MetaAhorro;
import com.finanzas.finanzas.repository.HistorialPrecioRepository;
import com.finanzas.finanzas.repository.MetaAhorroRepository;
import com.finanzas.finanzas.service.MercadoLibreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller adicional para manejar las rutas especificas de seguimiento
 * inteligente en las Metas de Ahorro.
 */
@RestController
@RequestMapping("/api/metas")
@CrossOrigin(origins = "*")
public class MetaAhorroSeguimientoController {

    @Autowired
    private MercadoLibreService mercadoLibreService;

    @Autowired
    private MetaAhorroRepository metaAhorroRepository;

    @Autowired
    private HistorialPrecioRepository historialPrecioRepository;

    // POST /api/metas/{usuarioId}/buscar-productos
    @PostMapping("/{usuarioId}/buscar-productos")
    public ResponseEntity<?> buscarProductos(@PathVariable Long usuarioId, @RequestBody BusquedaProductoRequest request) {
        try {
            if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "El termino de busqueda no puede estar vacio."));
            }
            List<ProductoMLDTO> resultados = mercadoLibreService.buscarProductos(request.getQuery());
            return ResponseEntity.ok(resultados);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // PUT /api/metas/{id}/activar-seguimiento
    @PutMapping("/{id}/activar-seguimiento")
    public ResponseEntity<?> activarSeguimiento(@PathVariable Long id, @RequestBody SeguimientoRequest request) {
        try {
            Optional<MetaAhorro> metaOpt = metaAhorroRepository.findById(id);
            if (metaOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            MetaAhorro meta = metaOpt.get();

            // Si estan desactivando el seguimiento
            if (request.getActivo() != null && !request.getActivo()) {
                meta.setSeguimientoActivo(false);
                metaAhorroRepository.save(meta);
                return ResponseEntity.ok(meta);
            }

            // Si estan activando/actualizando
            if (request.getExternalProductId() == null || request.getExternalProductId().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Se requiere el ID del producto (externalProductId)."));
            }

            ProductoMLDTO productoML = null;
            String externalId = request.getExternalProductId().trim();
            boolean vinculacionManualPorBloqueo403 = false;
            String detalleErrorConsulta = null;

            // Si ML bloquea la consulta (ej: 403), permitimos guardar vinculacion manual.
            try {
                productoML = mercadoLibreService.obtenerProductoPorId(externalId);
            } catch (Exception e) {
                detalleErrorConsulta = e.getMessage();
                if (detalleErrorConsulta != null && detalleErrorConsulta.contains(" 403 ")) {
                    vinculacionManualPorBloqueo403 = true;
                } else {
                    return ResponseEntity.internalServerError().body(Map.of(
                            "error", "Error consultando Mercado Libre para validar el producto.",
                            "detalle", detalleErrorConsulta != null ? detalleErrorConsulta : "Sin detalle"
                    ));
                }
            }

            meta.setMarketplace("MERCADOLIBRE");
            meta.setExternalProductId(productoML != null ? productoML.getId() : externalId);

            if (productoML != null) {
                meta.setProductUrl(productoML.getLink());
                meta.setProductTitle(productoML.getTitulo());
                meta.setProductImageUrl(productoML.getImagenUrl());
                meta.setUltimoPrecioDetectado(productoML.getPrecio());
                meta.setUltimoPrecioOriginal(productoML.getPrecioOriginal());
                meta.setUltimoDescuentoPorcentaje(productoML.getDescuentoPorcentaje());

                // Solo actualizamos mejor precio historico si no existia o si este es mejor.
                if (meta.getMejorPrecioHistorico() == null || productoML.getPrecio() < meta.getMejorPrecioHistorico()) {
                    meta.setMejorPrecioHistorico(productoML.getPrecio());
                }
            } else {
                if (meta.getProductTitle() == null || meta.getProductTitle().isBlank()) {
                    meta.setProductTitle("Producto ML (" + externalId + ")");
                }
                if (meta.getProductUrl() == null || meta.getProductUrl().isBlank()) {
                    meta.setProductUrl("https://www.mercadolibre.com");
                }
                if (meta.getProductImageUrl() == null || meta.getProductImageUrl().isBlank()) {
                    meta.setProductImageUrl("https://http2.mlstatic.com/static/org-img/homesnw/mercado-libre.png");
                }
            }

            if (request.getPrecioObjetivoCompra() != null && request.getPrecioObjetivoCompra() > 0) {
                meta.setPrecioObjetivoCompra(request.getPrecioObjetivoCompra());
            }

            meta.setSeguimientoActivo(true);
            meta.setUltimaRevision(LocalDateTime.now());

            metaAhorroRepository.save(meta);

            // Guardar primer registro en historial solo si hubo lectura real de precio.
            if (productoML != null) {
                HistorialPrecio historial = new HistorialPrecio();
                historial.setMeta(meta);
                historial.setPrecio(productoML.getPrecio());
                historial.setPrecioOriginal(productoML.getPrecioOriginal());
                historial.setDescuentoPorcentaje(productoML.getDescuentoPorcentaje());
                historial.setTituloProducto(productoML.getTitulo());
                historial.setImagenUrl(productoML.getImagenUrl());
                historial.setLinkProducto(productoML.getLink());
                historial.setFechaRevision(LocalDateTime.now());
                historial.setMarketplace("MERCADOLIBRE");
                historialPrecioRepository.save(historial);
            }

            if (vinculacionManualPorBloqueo403) {
                return ResponseEntity.ok(Map.of(
                        "warning", "Vinculada sin validacion en vivo: Mercado Libre bloqueo temporalmente la consulta (403).",
                        "detalle", detalleErrorConsulta != null ? detalleErrorConsulta : "Bloqueo 403 sin detalle"
                ));
            }

            return ResponseEntity.ok(meta);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/metas/{id}/historial-precios
    @GetMapping("/{id}/historial-precios")
    public ResponseEntity<?> obtenerHistorialPrecios(@PathVariable Long id) {
        try {
            Optional<MetaAhorro> metaOpt = metaAhorroRepository.findById(id);
            if (metaOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            List<HistorialPrecio> historial = historialPrecioRepository.findByMetaOrderByFechaRevisionDesc(metaOpt.get());
            return ResponseEntity.ok(historial);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
