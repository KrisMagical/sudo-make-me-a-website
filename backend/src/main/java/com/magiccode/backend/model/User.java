package com.magiccode.backend.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) // JSON 响应永远不返回 password
    @ToString.Exclude // Lombok toString 不打印 password（避免日志泄漏）
    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role;
}
