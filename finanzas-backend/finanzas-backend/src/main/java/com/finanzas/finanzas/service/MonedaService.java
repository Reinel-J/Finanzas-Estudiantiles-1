package com.finanzas.finanzas.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Conversión de moneda usando ExchangeRate-API.
 *
 * - Si se configura una API key (propiedad moneda.api-key o variable de entorno
 *   MONEDA_API_KEY), se usa el endpoint v6 con key.
 * - Si no, se usa el endpoint abierto sin key (open.er-api.com), que se
 *   actualiza una vez al día. Antes la key estaba fija como "YOUR_API_KEY_HERE"
 *   y toda conversión fallaba con 403.
 *
 * Las tasas se guardan en caché 1 hora por moneda base para no agotar el límite
 * de peticiones del servicio.
 */
@Service
public class MonedaService {

    private static final Duration CACHE_TTL = Duration.ofHours(1);

    private final RestTemplate restTemplate = new RestTemplate();
    private final Map<String, TasasCacheadas> cache = new ConcurrentHashMap<>();

    @Value("${moneda.api-key:}")
    private String apiKey;

    private record TasasCacheadas(Map<String, Object> tasas, Instant obtenidas) {}

    public Map<String, Object> convertir(double monto, String monedaOrigen, String monedaDestino) {
        String origen = normalizar(monedaOrigen);
        String destino = normalizar(monedaDestino);

        double tasa;
        if (origen.equals(destino)) {
            tasa = 1.0;
        } else {
            Object valor = obtenerTasas(origen).get(destino);
            if (!(valor instanceof Number)) {
                throw new IllegalArgumentException("Moneda de destino no soportada: " + destino);
            }
            tasa = ((Number) valor).doubleValue();
        }

        double montoConvertido = monto * tasa;
        return Map.of(
                "montoOriginal", monto,
                "monedaOrigen", origen,
                "montoConvertido", Math.round(montoConvertido * 100.0) / 100.0,
                "monedaDestino", destino,
                "tasa", tasa
        );
    }

    private Map<String, Object> obtenerTasas(String base) {
        TasasCacheadas enCache = cache.get(base);
        if (enCache != null && enCache.obtenidas().plus(CACHE_TTL).isAfter(Instant.now())) {
            return enCache.tasas();
        }

        boolean conKey = apiKey != null && !apiKey.isBlank();
        String url = conKey
                ? "https://v6.exchangerate-api.com/v6/" + apiKey.trim() + "/latest/" + base
                : "https://open.er-api.com/v6/latest/" + base;

        Map<?, ?> respuesta;
        try {
            respuesta = restTemplate.getForObject(url, Map.class);
        } catch (RestClientException e) {
            throw new IllegalStateException("No se pudo consultar el servicio de tasas de cambio. Intenta más tarde.");
        }

        if (respuesta == null || !"success".equals(respuesta.get("result"))) {
            Object tipo = respuesta != null ? respuesta.get("error-type") : null;
            if ("unsupported-code".equals(tipo)) {
                throw new IllegalArgumentException("Moneda de origen no soportada: " + base);
            }
            throw new IllegalStateException("El servicio de tasas de cambio respondió con error"
                    + (tipo != null ? " (" + tipo + ")" : "") + ".");
        }

        // v6 con key devuelve "conversion_rates"; el endpoint abierto, "rates"
        Object rates = respuesta.containsKey("conversion_rates") ? respuesta.get("conversion_rates") : respuesta.get("rates");
        if (!(rates instanceof Map)) {
            throw new IllegalStateException("Respuesta inesperada del servicio de tasas de cambio.");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> tasas = (Map<String, Object>) rates;
        cache.put(base, new TasasCacheadas(tasas, Instant.now()));
        return tasas;
    }

    // Solo códigos ISO de 3 letras: evita que el valor se inyecte en la URL
    private static String normalizar(String codigo) {
        if (codigo == null || !codigo.trim().matches("[A-Za-z]{3}")) {
            throw new IllegalArgumentException("Código de moneda inválido: " + codigo);
        }
        return codigo.trim().toUpperCase();
    }
}
