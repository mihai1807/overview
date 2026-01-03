package com.mihai.overview.entity;

import jakarta.persistence.*;
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
                @Index(name = "ix_reviewed_user_id_interaction_score", columnList = "reviewed_user_id, interaction_score")
        }
)
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @Column(name = "review_type", nullable = false)
    private String reviewType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn (name ="reviewed_user_id", nullable = false)
    private User reviewedUser;

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

    @ManyToOne (fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    public Review(String reviewType, User reviewedUser, Long ticketId, LocalDate interactionDate, LocalTime interactionTime, Long cid, int interactionScore, boolean accepted, User reviewer) {
        this.reviewType = reviewType;
        this.reviewedUser = reviewedUser;
        this.ticketId = ticketId;
        this.interactionDate = interactionDate;
        this.interactionTime = interactionTime;
        this.cid = cid;
        this.interactionScore = interactionScore;
        this.accepted = accepted;
        this.reviewer = reviewer;
    }
}
