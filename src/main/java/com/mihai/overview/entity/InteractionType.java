package com.mihai.overview.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "interaction_types",
        indexes = {
                @Index(name = "ix_interaction_types_code", columnList = "code")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_interaction_types_code", columnNames = "code")
        }
)
public class InteractionType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    // e.g. CHAT, EMAIL, KYC
    @Column(nullable = false, length = 50, unique = true)
    private String code;

    // display name
    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(nullable = false)
    private boolean archived = false;
}
