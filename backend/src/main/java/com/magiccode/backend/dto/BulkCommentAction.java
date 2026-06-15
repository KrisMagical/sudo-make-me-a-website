package com.magiccode.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Bulk moderation action.", allowableValues = {"APPROVE", "REJECT", "DELETE"})
public enum BulkCommentAction {
    APPROVE,
    REJECT,
    DELETE
}
