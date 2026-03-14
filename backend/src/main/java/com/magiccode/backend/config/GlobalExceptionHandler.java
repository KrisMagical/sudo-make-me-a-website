package com.magiccode.backend.config;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, Object> badCredentials(BadCredentialsException e) {
        return Map.of("message", "Username or password incorrect.");
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, Object> authException(AuthenticationException e) {
        return Map.of("message", "Authentication failed");
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException ex, WebRequest request) {
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> handleDataIntegrityViolation(DataIntegrityViolationException ex, WebRequest request) {
        Map<String, String> error = new HashMap<>();
        if (ex.getMessage().contains("Duplicate entry")) {
            error.put("error", "Slug already exists, please use another one.");
        } else {
            error.put("error", "Data integrity violation.");
        }
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
}
