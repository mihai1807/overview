package com.mihai.overview.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class CriticalConditionPoolItemResponse {
    private Long id;
    private Long interactionTypeId;
    private String name;
    private String description;
}
