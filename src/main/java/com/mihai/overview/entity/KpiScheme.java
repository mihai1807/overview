package com.mihai.overview.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "kpi_schemes",
        indexes = {
                @Index(name = "ix_schemes_review_type", columnList = "review_type_id")
        }
)
public class KpiScheme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_type_id", nullable = false)
    private ReviewType reviewType;

    @Column(nullable = false, length = 100)
    private String name; // e.g. chat1, chat2

    @Column(nullable = false)
    private boolean active = false;

    @OneToMany(mappedBy = "scheme", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<KpiSchemeItem> items = new HashSet<>();
}
