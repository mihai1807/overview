package com.mihai.overview.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "review_critical_hits",
        uniqueConstraints = @UniqueConstraint(name = "uq_review_critical", columnNames = {"review_id", "critical_id"}),
        indexes = {
                @Index(name = "ix_review_critical_hits_review", columnList = "review_id"),
                @Index(name = "ix_review_critical_hits_critical", columnList = "critical_id")
        }
)
public class ReviewCriticalHit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "critical_id", nullable = false)
    private CriticalConditionPoolItem critical;

    @Column(nullable = false)
    private boolean triggered;
}
