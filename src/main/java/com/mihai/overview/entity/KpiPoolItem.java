package com.mihai.overview.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "kpi_pool_items",
        indexes = {
                @Index(name = "ix_kpi_pool_type", columnList = "interaction_type_id"),
                @Index(name = "ix_kpi_pool_name", columnList = "name")
        }
)
public class KpiPoolItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interaction_type_id", nullable = false)
    private InteractionType interactionType;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 500)
    private String description;

    // rubric / scoring guidance
    @Column(nullable = false, columnDefinition = "TEXT")
    private String details;

    // fixed weight in the pool (your decision)
    @Column(nullable = false)
    private int weightPercent; // 0..100

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(nullable = false)
    private boolean archived = false;
}
