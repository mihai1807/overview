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
        uniqueConstraints = @UniqueConstraint(
                name = "uq_review_scheme_item",
                columnNames = {"review_id", "scheme_item_id"}
        ),
        indexes = {
                @Index(name = "ix_review_kpi_scores_review", columnList = "review_id"),
                @Index(name = "ix_review_kpi_scores_item", columnList = "scheme_item_id")
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
    @JoinColumn(name = "scheme_item_id", nullable = false)
    private KpiSchemeItem schemeItem;

    @Column(nullable = false)
    private int score;
}
