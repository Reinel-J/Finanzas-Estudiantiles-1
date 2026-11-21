package com.finanzas.finanzas;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

/**
 * Convierte cualquier excepción no capturada por los controladores en una
 * respuesta JSON con la forma { "error": "mensaje" }, que es lo que espera
 * fetchAPI() en el frontend. Antes, errores como "Usuario no encontrado"
 * terminaban en un 500 genérico sin mensaje.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> jsonInvalido(HttpMessageNotReadableException e) {
        return error(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es un JSON válido o tiene tipos incorrectos.");
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, String>> parametroInvalido(Exception e) {
        return error(HttpStatus.BAD_REQUEST, "Parámetro inválido o faltante: " + e.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> integridad(DataIntegrityViolationException e) {
        log.warn("Violación de integridad de datos: {}", e.getMostSpecificCause().getMessage());
        return error(HttpStatus.BAD_REQUEST, "Datos inválidos: faltan campos obligatorios o hay un valor duplicado.");
    }

    // Errores de negocio (ej: "Usuario no encontrado.", "Saldo insuficiente...")
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, String>> negocio(RuntimeException e) {
        return error(HttpStatus.BAD_REQUEST, mensaje(e, "Solicitud inválida."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> general(Exception e) {
        // Respeta el código de las excepciones propias de Spring (404 de rutas, 405, etc.)
        if (e instanceof ErrorResponse er) {
            HttpStatusCode status = er.getStatusCode();
            return ResponseEntity.status(status).body(Map.of("error", mensaje(e, "Error en la petición.")));
        }
        if (e instanceof RuntimeException) {
            log.warn("Error procesando la petición: {}", e.toString());
            return error(HttpStatus.BAD_REQUEST, mensaje(e, "Solicitud inválida."));
        }
        log.error("Error interno no controlado", e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor.");
    }

    private static String mensaje(Exception e, String porDefecto) {
        return (e.getMessage() == null || e.getMessage().isBlank()) ? porDefecto : e.getMessage();
    }

    private static ResponseEntity<Map<String, String>> error(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status).body(Map.of("error", mensaje));
    }
}
