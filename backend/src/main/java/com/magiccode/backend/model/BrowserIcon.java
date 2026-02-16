package com.magiccode.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "browser_icons")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrowserIcon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "favicon_image_id")
    private Long faviconImageId;
    @Column(name = "apple_touch_icon_image_id")
    private Long appleTouchIconImageId;
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
