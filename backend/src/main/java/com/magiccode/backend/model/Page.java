package com.magiccode.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 200, unique = true)
    private String slug;
    @Column(nullable = false, length = 200)
    private String title;
    @Lob
    @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;
}
