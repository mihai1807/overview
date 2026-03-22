package com.mihai.overview.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class SchemeResponse {
    private Long id;
    private Long interactionTypeId;
    private String name;
}
