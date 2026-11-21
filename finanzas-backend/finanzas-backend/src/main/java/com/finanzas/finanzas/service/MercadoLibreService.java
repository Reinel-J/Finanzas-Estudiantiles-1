package com.finanzas.finanzas.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finanzas.finanzas.dto.ProductoMLDTO;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@Service
public class MercadoLibreService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MercadoLibreOAuthService oauthService;
    private static final String ML_SEARCH_API_TEMPLATE = "https://api.mercadolibre.com/sites/%s/search";
    private static final List<String> ML_SITES_FALLBACK = List.of("MCO");

    public MercadoLibreService(MercadoLibreOAuthService oauthService) {
        this.oauthService = oauthService;
    }

    public List<ProductoMLDTO> buscarProductos(String query) {
        RuntimeException ultimoError = null;

        for (String site : ML_SITES_FALLBACK) {
            // encode() + URI: la búsqueda se codifica una sola vez (antes un "&" en la
            // búsqueda se interpretaba como otro parámetro)
            java.net.URI url = UriComponentsBuilder
                    .fromHttpUrl(String.format(ML_SEARCH_API_TEMPLATE, site))
                    .queryParam("q", query)
                    .queryParam("limit", 10)
                    .encode()
                    .build()
                    .toUri();
            List<ProductoMLDTO> productos = new ArrayList<>();

            try {
                ResponseEntity<String> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        new HttpEntity<>(buildHeaders()),
                        String.class);
                String responseStr = response.getBody();
                if (responseStr != null) {
                    JsonNode root = objectMapper.readTree(responseStr);
                    JsonNode results = root.path("results");

                    for (int i = 0; i < Math.min(results.size(), 10); i++) {
                        JsonNode item = results.get(i);
                        ProductoMLDTO producto = parseItemML(item);
                        if (producto != null) {
                            productos.add(producto);
                        }
                    }
                }

                // Si este sitio funcionó (aunque sin resultados), devolvemos y no seguimos
                // probando.
                return productos;
            } catch (HttpStatusCodeException e) {
                ultimoError = new RuntimeException("Mercado Libre respondió " + e.getStatusCode().value() +
                        " al buscar productos en sitio " + site + ".");
                // Si es 403, intentamos con siguiente sitio; para otros errores también
                // continuamos fallback.
            } catch (Exception e) {
                ultimoError = new RuntimeException(
                        "Error buscando en Mercado Libre (sitio " + site + "): " + e.getMessage(), e);
            }
        }

        if (ultimoError != null) {
            throw ultimoError;
        }
        throw new RuntimeException("No fue posible consultar Mercado Libre.");
    }

    public ProductoMLDTO obtenerProductoPorId(String id) {
        if (id == null || !id.matches("[A-Za-z]{3}\\d+")) {
            throw new IllegalArgumentException("ID de producto de Mercado Libre inválido: " + id);
        }
        String url = "https://api.mercadolibre.com/items/" + id;
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(buildHeaders()),
                    String.class);
            String responseStr = response.getBody();
            if (responseStr != null) {
                JsonNode item = objectMapper.readTree(responseStr);
                return parseItemML(item);
            }
        } catch (HttpStatusCodeException e) {
            throw new RuntimeException("Mercado Libre respondió " + e.getStatusCode().value() +
                    " al consultar el producto " + id + ".");
        } catch (Exception e) {
            throw new RuntimeException("Error buscando producto por id en Mercado Libre: " + e.getMessage(), e);
        }
        return null;
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        headers.set("Accept", "application/json");

        String token = oauthService.getValidAccessToken();
        if (token != null && !token.isBlank()) {
            headers.setBearerAuth(token);
        }
        return headers;
    }

    private ProductoMLDTO parseItemML(JsonNode item) {
        try {
            String id = item.path("id").asText();
            String title = item.path("title").asText();
            double price = item.path("price").asDouble();
            String permalink = item.path("permalink").asText();
            String thumbnail = item.path("thumbnail").asText().replace("-I.jpg", "-O.jpg"); // Better quality image
            String currency = item.path("currency_id").asText();
            String condition = item.path("condition").asText();
            String sellerName = item.path("seller").path("nickname").asText();

            // ML occasionally hides original_price. Check if available.
            Double originalPrice = null;
            // hasNonNull: si el campo no existe, path() devolvía 0.0 como precio original
            if (item.hasNonNull("original_price")) {
                originalPrice = item.path("original_price").asDouble();
            }

            double discountPercentage = 0.0;
            if (originalPrice != null && originalPrice > price) {
                discountPercentage = ((originalPrice - price) / originalPrice) * 100.0;
                discountPercentage = Math.round(discountPercentage * 100.0) / 100.0; // round to 2 decimals
            }

            return ProductoMLDTO.builder()
                    .id(id)
                    .titulo(title)
                    .precio(price)
                    .precioOriginal(originalPrice)
                    .descuentoPorcentaje(discountPercentage)
                    .imagenUrl(thumbnail)
                    .link(permalink)
                    .vendedor(sellerName)
                    .condicion(condition)
                    .moneda(currency)
                    .build();
        } catch (Exception e) {
            System.err.println("Error parsing ML item: " + e.getMessage());
            return null;
        }
    }
}
