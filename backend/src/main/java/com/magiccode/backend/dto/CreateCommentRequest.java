package com.magiccode.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Visitor or admin comment creation request.")
public class CreateCommentRequest {
    @Schema(description = "Display name for the comment author.", example = "Reader")
    @NotBlank(message = "name must not be blank")
    @Size(max = 50, message = "name must be 50 characters or less")
    private String name;

    @Schema(description = "Email address used for moderation context. It is not shown in public comment lists.", example = "reader@example.com")
    @NotBlank(message = "email must not be blank")
    @Email
    @Size(max = 120, message = "email must be 120 characters or less")
    private String email;

    @Schema(description = "Comment body. Blank or whitespace-only content is rejected.", example = "Thanks for the post.")
    @NotBlank(message = "content must not be blank")
    @Size(max = 2000, message = "content must be 2000 characters or less")
    private String content;

    @Schema(description = "Optional parent comment id for replies.", example = "42", nullable = true)
    private Long parentId;
}
