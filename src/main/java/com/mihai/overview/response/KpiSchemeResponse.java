package com.mihai.overview.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class KpiSchemeResponse {
    private Long id;
    private Long reviewTypeId;
    private String name;
    private boolean active;
}
