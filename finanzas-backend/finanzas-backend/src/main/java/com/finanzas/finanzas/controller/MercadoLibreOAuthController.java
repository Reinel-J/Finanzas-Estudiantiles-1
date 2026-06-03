package com.finanzas.finanzas.controller;

import com.finanzas.finanzas.service.MercadoLibreOAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ml")
@CrossOrigin(origins = "*")
public class MercadoLibreOAuthController {

    @Autowired
    private MercadoLibreOAuthService oauthService;

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/auth-url")
    public ResponseEntity<?> getAuthUrl() {
        try {
            return ResponseEntity.ok(Map.of("url", oauthService.buildAuthorizationUrl()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/callback")
    public ResponseEntity<?> callback(@RequestParam(required = false) String code,
                                      @RequestParam(required = false) String error,
                                      @RequestParam(required = false) String error_description) {
        try {
            if (error != null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", error,
                        "description", error_description != null ? error_description : "Autorizacion rechazada."
                ));
            }
            if (code == null || code.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No llego 'code' en callback."));
            }
            Map<String, Object> tokenPayload = oauthService.exchangeCodeForToken(code);
            return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "message", "Token de Mercado Libre obtenido correctamente.",
                    "expires_in", tokenPayload.get("expires_in")
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<?> status() {
        try {
            return ResponseEntity.ok(oauthService.getStatus());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh() {
        try {
            oauthService.refreshAccessToken();
            return ResponseEntity.ok(Map.of("ok", true, "message", "Access token renovado."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/diag")
    public ResponseEntity<?> diag(@RequestParam(defaultValue = "MCO1055308835") String itemId,
                                  @RequestParam(defaultValue = "gtx") String q) {
        try {
            String token = oauthService.getValidAccessToken();
            if (token == null || token.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No hay access token activo. Autoriza en /api/ml/auth-url."));
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.set("Accept", "application/json");
            headers.set("User-Agent", "FinanzasApp/1.0 (+https://localhost)");
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            Map<String, Object> out = new LinkedHashMap<>();
            out.put("token_status", oauthService.getStatus());
            out.put("users_me", probe("https://api.mercadolibre.com/users/me", entity));
            out.put("item", probe("https://api.mercadolibre.com/items/" + itemId, entity));
            out.put("search_mco", probe("https://api.mercadolibre.com/sites/MCO/search?q=" + q + "&limit=1", entity));
            out.put("search_mla", probe("https://api.mercadolibre.com/sites/MLA/search?q=" + q + "&limit=1", entity));
            out.put("search_mlm", probe("https://api.mercadolibre.com/sites/MLM/search?q=" + q + "&limit=1", entity));

            return ResponseEntity.ok(out);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    private Map<String, Object> probe(String url, HttpEntity<Void> entity) {
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return Map.of(
                    "ok", true,
                    "status", response.getStatusCode().value()
            );
        } catch (HttpStatusCodeException e) {
            Map<String, Object> fail = new LinkedHashMap<>();
            fail.put("ok", false);
            fail.put("status", e.getStatusCode().value());
            fail.put("body", e.getResponseBodyAsString());
            return fail;
        } catch (Exception e) {
            return Map.of(
                    "ok", false,
                    "status", -1,
                    "error", e.getMessage()
            );
        }
    }
}
