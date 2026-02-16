package com.magiccode.backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "page_links", uniqueConstraints = {
        @UniqueConstraint(name = "uk_page_links_from_to", columnNames = {"from_page_id", "to_page_id"})
},
        indexes = {
                @Index(name = "idx_page_links_from", columnList = "from_page_id"),
                @Index(name = "index_page_links_to", columnList = "to_page_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageLink {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_page_id", nullable = false)
    private Page fromPage;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_page_id", nullable = false)
    private Page toPage;
}
