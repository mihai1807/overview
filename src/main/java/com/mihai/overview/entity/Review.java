package com.mihai.overview.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "reviews",
        indexes = {
                @Index(name = "ix_reviews_scheme", columnList = "scheme_id"),
                @Index(name = "ix_reviews_reviewed_period", columnList = "reviewed_user_id, period_key"),
                @Index(name = "ix_reviews_reviewer", columnList = "reviewer_id"),
                @Index(name = "ix_reviews_occurred", columnList = "occurred_at")
        }
)
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scheme_id", nullable = false)
    private Scheme scheme;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interaction_type_id", nullable = false)
    private InteractionType interactionType;

    @Column(name = "reviewed_user_id", nullable = false)
    private Long reviewedUserId;

    @Column(name = "reviewer_id", nullable = false)
    private Long reviewerId;

    @Column(name = "ticket_id")
    private Long ticketId;

    @Column(name = "cid")
    private Long cid;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    // "YYYY-MM"
    @Column(name = "period_key", nullable = false, length = 7)
    private String periodKey;

    @Column(name = "total_score", nullable = false)
    private int totalScore; // 0..100

    @Column(nullable = false)
    private boolean accepted = true;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ReviewKpiScore> kpiScores = new HashSet<>();

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ReviewCriticalHit> criticalHits = new HashSet<>();
}
