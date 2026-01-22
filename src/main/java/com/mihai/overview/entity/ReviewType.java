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
@Table(name = "review_types",
        indexes = {
                @Index(name = "ix_review_types_code", columnList = "code")
        }
)
public class ReviewType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    // e.g. CHAT, EMAIL
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    // display label
    @Column(nullable = false, length = 100)
    private String name;

    // active scheme for this type
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "active_scheme_id")
    private KpiScheme activeScheme;

    @OneToMany(mappedBy = "reviewType", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<InteractionKpi> kpis = new HashSet<>();

    @OneToMany(mappedBy = "reviewType", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<KpiScheme> schemes = new HashSet<>();

    @Column(name = "created_by_user_id")
    private Long createdByUserId;
}
