package com.mihai.overview.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "schemes",
        indexes = {
                @Index(name = "ix_schemes_type", columnList = "interaction_type_id"),
                @Index(name = "ix_schemes_name", columnList = "name")
        }
)
public class Scheme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interaction_type_id", nullable = false)
    private InteractionType interactionType;

    @Column(nullable = false, length = 120)
    private String name; // e.g. "Chat v1"

    // KPI selections (stored in join table)
    @ElementCollection
    @CollectionTable(
            name = "scheme_kpi_rules",
            joinColumns = @JoinColumn(name = "scheme_id"),
            uniqueConstraints = @UniqueConstraint(name = "uq_scheme_kpi_rules", columnNames = {"scheme_id", "kpi_id"}),
            indexes = {
                    @Index(name = "ix_scheme_kpi_rules_scheme", columnList = "scheme_id"),
                    @Index(name = "ix_scheme_kpi_rules_kpi", columnList = "kpi_id")
            }
    )
    private List<SchemeKpiRule> kpis = new ArrayList<>();

    // Critical selections (stored in join table)
    @ElementCollection
    @CollectionTable(
            name = "scheme_critical_rules",
            joinColumns = @JoinColumn(name = "scheme_id"),
            uniqueConstraints = @UniqueConstraint(name = "uq_scheme_critical_rules", columnNames = {"scheme_id", "critical_id"}),
            indexes = {
                    @Index(name = "ix_scheme_critical_rules_scheme", columnList = "scheme_id"),
                    @Index(name = "ix_scheme_critical_rules_critical", columnList = "critical_id")
            }
    )
    private List<SchemeCriticalRule> criticals = new ArrayList<>();

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(nullable = false)
    private boolean archived = false;
}
