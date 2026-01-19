package com.mihai.overview.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@Entity

@Table(name = "reviews",
        indexes = {
                @Index(name = "ix_reviews_reviewer", columnList = "reviewer_id"),
                @Index(name = "ix_reviews_reviewed", columnList = "reviewed_user_id"),
                @Index(name = "ix_reviews_date", columnList = "interaction_date"),
                @Index(name = "ix_reviews_reviewer_date", columnList = "reviewer_id, interaction_date"),
                @Index(name = "ix_reviewed_user_id_interaction_score", columnList = "reviewed_user_id, interaction_score"),
                @Index(name = "ix_reviews_kpi_scheme", columnList = "kpi_scheme_id")
        }
)
public class Review {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @Column(name = "review_type", nullable = false)
    private String reviewType;


    @Column (name ="reviewed_user_id", nullable = false)
    private Long reviewedUserId;

    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;

    @Column(name = "interaction_date", nullable = false)
    private LocalDate interactionDate;

    @Column(name = "interaction_time", nullable = false)
    private LocalTime interactionTime;

    @Column(nullable = false)
    private Long cid;

    @Column(name = "interaction_score", nullable = false)
    private int interactionScore;

    @Column(nullable = false)
    private boolean accepted;

    @Column(name = "reviewer_id", nullable = false)
    private Long reviewerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kpi_scheme_id")
    private KpiScheme kpiScheme;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.Set<ReviewKpiScore> kpiScores = new java.util.HashSet<>();

    public Review(String reviewType, Long reviewedUserId, Long ticketId, LocalDate interactionDate, LocalTime interactionTime, Long cid, int interactionScore, boolean accepted, Long reviewerID) {
        this.reviewType = reviewType;
        this.reviewedUserId = reviewedUserId;
        this.ticketId = ticketId;
        this.interactionDate = interactionDate;
        this.interactionTime = interactionTime;
        this.cid = cid;
        this.interactionScore = interactionScore;
        this.accepted = accepted;
        this.reviewerId = reviewerID;
    }

}
