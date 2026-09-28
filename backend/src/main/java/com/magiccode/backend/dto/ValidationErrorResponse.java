package com.magiccode.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(name = "ValidationErrorResponse", description = "Validation failure response.")
public class ValidationErrorResponse {
    @Schema(description = "Validation summary.", example = "Validation failed")
    public String message;

    @Schema(description = "Field-level validation errors.", example = "{\"email\":\"must be a well-formed email address\"}")
    public Map<String, String> errors;
}

