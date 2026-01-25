package com.mihai.overview.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "critical_condition_pool_items",
        indexes = {
                @Index(name = "ix_critical_pool_type", columnList = "interaction_type_id"),
                @Index(name = "ix_critical_pool_name", columnList = "name")
        }
)
public class CriticalConditionPoolItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interaction_type_id", nullable = false)
    private InteractionType interactionType;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(nullable = false)
    private boolean archived = false;
}
