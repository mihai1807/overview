package com.mihai.overview.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class InteractionKpiResponse {
    private Long id;
    private Long reviewTypeId;
    private String name;
    private String description;
    private boolean active;
}
