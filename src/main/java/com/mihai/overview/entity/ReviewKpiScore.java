package com.mihai.overview.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "review_kpi_scores",
        uniqueConstraints = @UniqueConstraint(name = "uq_review_kpi", columnNames = {"review_id", "kpi_id"}),
        indexes = {
                @Index(name = "ix_review_kpi_scores_review", columnList = "review_id"),
                @Index(name = "ix_review_kpi_scores_kpi", columnList = "kpi_id")
        }
)
public class ReviewKpiScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kpi_id", nullable = false)
    private KpiPoolItem kpi;

    @Column(nullable = true)
    private Integer score; // null until filled, then 0..100
}
