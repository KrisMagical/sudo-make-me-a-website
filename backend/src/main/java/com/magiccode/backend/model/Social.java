package com.magiccode.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "socials")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Social {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, unique = true)
    private String name;
    @Column(nullable = false, length = 300)
    private String url;

    @Column(length = 500)
    private String description;

    @Column(name = "icon_image_id")
    private Long iconImageId;
    @Column(name = "external_icon_url", length = 1000)
    private String externalIconUrl;
    @Column(name = "icon_url", length = 1000)
    private String iconUrl;
}
