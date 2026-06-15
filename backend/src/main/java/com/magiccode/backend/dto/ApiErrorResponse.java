package com.magiccode.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(name = "ApiErrorResponse", description = "Standard API error response.")
public class ApiErrorResponse {
    @Schema(description = "Human-readable error message.", example = "Request failed")
    public String message;

    @Schema(description = "Optional machine-readable error code.", example = "CONFLICT")
    public String code;

    @Schema(description = "Field-level validation errors. Empty for non-validation errors.")
    public Map<String, String> errors;
}

