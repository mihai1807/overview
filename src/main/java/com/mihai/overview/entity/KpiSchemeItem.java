package com.mihai.overview.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "kpi_scheme_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_scheme_kpi",
                columnNames = {"scheme_id", "kpi_id"}
        ),
        indexes = {
                @Index(name = "ix_scheme_items_scheme", columnList = "scheme_id"),
                @Index(name = "ix_scheme_items_kpi", columnList = "kpi_id")
        }
)
public class KpiSchemeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scheme_id", nullable = false)
    private KpiScheme scheme;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kpi_id", nullable = false)
    private InteractionKpi kpi;

    @Column(nullable = false)
    private int minScore; // e.g. 0

    @Column(nullable = false)
    private int maxScore; // e.g. 30

    // 0..100, sum of scheme items must be 100 (you wanted that)
    @Column(nullable = false)
    private int weightPercent;

    @Column(nullable = false)
    private int orderIndex;

    @Column(nullable = false)
    private boolean required = true;
}
