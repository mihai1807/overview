package com.mihai.overview.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "interaction_kpis",
        indexes = {
                @Index(name = "ix_kpis_review_type", columnList = "review_type_id"),
                @Index(name = "ix_kpis_name", columnList = "name")
        }
)
public class InteractionKpi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_type_id", nullable = false)
    private ReviewType reviewType;

    @Column(nullable = false, length = 100)
    private String name; // e.g. clarity, tone

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(nullable = false)
    private boolean archived = false;
}
