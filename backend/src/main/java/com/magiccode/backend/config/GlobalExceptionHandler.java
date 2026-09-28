package com.magiccode.backend.config;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.server.ResponseStatusException;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, Object> badCredentials(BadCredentialsException e) {
        log.warn("authentication failed reason=bad_credentials");
        return apiError("Username or password incorrect.");
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, Object> authException(AuthenticationException e) {
        log.warn("authentication failed type={}", e.getClass().getSimpleName());
        return apiError("Authentication failed");
    }

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, Object> accessDenied(RuntimeException e) {
        log.warn("access denied type={}", e.getClass().getSimpleName());
        return apiError("Forbidden");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        log.warn("validation failed fields={}", errors.keySet());
        return ResponseEntity.badRequest().body(apiError("Validation failed", errors));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> handleResponseStatusException(ResponseStatusException ex, WebRequest request) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String reason = ex.getReason() == null ? status.getReasonPhrase() : ex.getReason();
        log.warn("request failed status={} type={} message={}", status.value(), ex.getClass().getSimpleName(), reason);
        return new ResponseEntity<>(apiError(reason), status);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException ex, WebRequest request) {
        HttpStatus status = ex.getMessage() != null && ex.getMessage().toLowerCase().contains("already")
                ? HttpStatus.CONFLICT
                : HttpStatus.BAD_REQUEST;
        log.warn("request failed status={} type={} message={}", status.value(), ex.getClass().getSimpleName(), ex.getMessage());
        return new ResponseEntity<>(apiError(ex.getMessage()), status);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> handleDataIntegrityViolation(DataIntegrityViolationException ex, WebRequest request) {
        String message;
        if (ex.getMessage().contains("Duplicate entry")) {
            message = "Duplicate value already exists.";
        } else {
            message = "Data integrity violation.";
        }
        log.warn("data integrity violation message={}", message);
        return new ResponseEntity<>(apiError(message), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<?> handleNoResourceFound(NoResourceFoundException ex, WebRequest request) {
        log.warn("resource not found path={}", ex.getResourcePath());
        return new ResponseEntity<>(apiError("Not found"), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleUnexpectedException(Exception ex, WebRequest request) {
        log.error("unexpected server error type={}", ex.getClass().getSimpleName(), ex);
        return new ResponseEntity<>(apiError("Internal server error"), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private Map<String, Object> apiError(String message) {
        return apiError(message, Map.of());
    }

    private Map<String, Object> apiError(String message, Map<String, String> errors) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", message == null ? "Request failed" : message);
        body.put("errors", errors);
        return body;
    }
}
