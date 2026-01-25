package com.mihai.overview.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class SchemeCriticalRule {

    @Column(name = "critical_id", nullable = false)
    private Long criticalId;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Column(name = "archived", nullable = false)
    private boolean archived = false;
}

