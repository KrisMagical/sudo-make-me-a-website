package com.magiccode.backend.dto;

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
public class CreateCommentRequest {
    @NotBlank(message = "name must not be blank")
    @Size(max = 50, message = "name must be 50 characters or less")
    private String name;

    @NotBlank(message = "email must not be blank")
    @Email
    @Size(max = 120, message = "email must be 120 characters or less")
    private String email;

    @NotBlank(message = "content must not be blank")
    @Size(max = 2000, message = "content must be 2000 characters or less")
    private String content;

    private Long parentId;
}
