package com.finanzas.finanzas.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class MonedaService {

    private static final String API_KEY = "YOUR_API_KEY_HERE";
    private static final String BASE_URL = "https://v6.exchangerate-api.com/v6/" + API_KEY + "/pair/";

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> convertir(double monto, String monedaOrigen, String monedaDestino) {
        String url = BASE_URL + monedaOrigen.toUpperCase() + "/" + monedaDestino.toUpperCase();

        Map<?, ?> respuesta = restTemplate.getForObject(url, Map.class);

        if (respuesta == null || !respuesta.containsKey("conversion_rate")) {
            throw new RuntimeException("Error al obtener la tasa de cambio.");
        }

        double tasa = ((Number) respuesta.get("conversion_rate")).doubleValue();
        double montoConvertido = monto * tasa;

        return Map.of(
                "montoOriginal", monto,
                "monedaOrigen", monedaOrigen.toUpperCase(),
                "montoConvertido", Math.round(montoConvertido * 100.0) / 100.0,
                "monedaDestino", monedaDestino.toUpperCase(),
                "tasa", tasa
        );
    }
}