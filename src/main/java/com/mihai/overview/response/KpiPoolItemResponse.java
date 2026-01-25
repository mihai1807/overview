package com.mihai.overview.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class KpiPoolItemResponse {
    private Long id;
    private Long interactionTypeId;
    private String name;
    private String description;
    private String details;
    private int weightPercent;
}
