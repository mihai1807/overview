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
public class SchemeKpiRule {

    @Column(name = "kpi_id", nullable = false)
    private Long kpiId;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Column(name = "required", nullable = false)
    private boolean required = true;

    @Column(name = "archived", nullable = false)
    private boolean archived = false;
}
