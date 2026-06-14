package com.magiccode.backend.config;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, Object> badCredentials(BadCredentialsException e) {
        return apiError("Username or password incorrect.");
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, Object> authException(AuthenticationException e) {
        return apiError("Authentication failed");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(apiError("Validation failed", errors));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException ex, WebRequest request) {
        HttpStatus status = ex.getMessage() != null && ex.getMessage().toLowerCase().contains("already")
                ? HttpStatus.CONFLICT
                : HttpStatus.BAD_REQUEST;
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
        return new ResponseEntity<>(apiError(message), HttpStatus.CONFLICT);
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
