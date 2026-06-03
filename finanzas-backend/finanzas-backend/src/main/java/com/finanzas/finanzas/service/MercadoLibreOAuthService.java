package com.finanzas.finanzas.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.security.MessageDigest;
import java.security.SecureRandom;

@Service
public class MercadoLibreOAuthService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ml.oauth.client-id:}")
    private String clientId;

    @Value("${ml.oauth.client-secret:}")
    private String clientSecret;

    @Value("${ml.oauth.redirect-uri:}")
    private String redirectUri;

    @Value("${ml.oauth.auth-base:https://auth.mercadolibre.com.co}")
    private String authBase;

    @Value("${ml.oauth.token-url:https://api.mercadolibre.com/oauth/token}")
    private String tokenUrl;

    private String accessToken;
    private String refreshToken;
    private Instant accessTokenExpiresAt;
    private String currentCodeVerifier;

    public String buildAuthorizationUrl() {
        validateConfigured();
        currentCodeVerifier = generateCodeVerifier();
        String codeChallenge = generateCodeChallenge(currentCodeVerifier);
        return UriComponentsBuilder
                .fromHttpUrl(authBase + "/authorization")
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", "S256")
                .build()
                .toUriString();
    }

    public Map<String, Object> exchangeCodeForToken(String code) {
        validateConfigured();
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("code", code);
        body.add("redirect_uri", redirectUri);
        if (currentCodeVerifier == null || currentCodeVerifier.isBlank()) {
            throw new RuntimeException("No hay code_verifier activo. Genera una nueva URL en /api/ml/auth-url y reintenta.");
        }
        body.add("code_verifier", currentCodeVerifier);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                tokenUrl,
                new HttpEntity<>(body, formHeaders()),
                Map.class
        );
        Map<String, Object> payload = response.getBody();
        if (payload == null) {
            throw new RuntimeException("Respuesta vacia al intercambiar code por token.");
        }
        saveTokenPayload(payload);
        currentCodeVerifier = null;
        return payload;
    }

    public synchronized String getValidAccessToken() {
        if (accessToken == null) return null;
        if (accessTokenExpiresAt != null && Instant.now().isAfter(accessTokenExpiresAt.minusSeconds(30))) {
            refreshAccessToken();
        }
        return accessToken;
    }

    public synchronized void refreshAccessToken() {
        validateConfigured();
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new RuntimeException("No hay refresh_token para renovar acceso.");
        }

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("refresh_token", refreshToken);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                tokenUrl,
                new HttpEntity<>(body, formHeaders()),
                Map.class
        );
        Map<String, Object> payload = response.getBody();
        if (payload == null) {
            throw new RuntimeException("Respuesta vacia al renovar access token.");
        }
        saveTokenPayload(payload);
    }

    public Map<String, Object> getStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("configured", isConfigured());
        status.put("authorized", accessToken != null);
        status.put("hasRefreshToken", refreshToken != null);
        status.put("expiresAt", accessTokenExpiresAt != null ? accessTokenExpiresAt.toString() : null);
        return status;
    }

    public boolean isConfigured() {
        return notBlank(clientId) && notBlank(clientSecret) && notBlank(redirectUri);
    }

    private void saveTokenPayload(Map<String, Object> payload) {
        Object at = payload.get("access_token");
        Object rt = payload.get("refresh_token");
        Object expiresIn = payload.get("expires_in");

        if (at == null) {
            throw new RuntimeException("No se recibio access_token.");
        }
        accessToken = String.valueOf(at);
        if (rt != null) refreshToken = String.valueOf(rt);

        long expires = 21600L;
        if (expiresIn instanceof Number n) {
            expires = n.longValue();
        } else if (expiresIn != null) {
            expires = Long.parseLong(String.valueOf(expiresIn));
        }
        accessTokenExpiresAt = Instant.now().plusSeconds(expires);
    }

    private HttpHeaders formHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    private void validateConfigured() {
        if (!isConfigured()) {
            throw new RuntimeException("Falta configurar ml.oauth.client-id, ml.oauth.client-secret o ml.oauth.redirect-uri.");
        }
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String generateCodeVerifier() {
        byte[] randomBytes = new byte[48];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String generateCodeChallenge(String verifier) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(verifier.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo generar PKCE code_challenge.", e);
        }
    }
}
