package com.mihai.overview.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class InteractionTypeResponse {
    private Long id;
    private String code;
    private String name;
}
