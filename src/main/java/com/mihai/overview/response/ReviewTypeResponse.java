package com.mihai.overview.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class ReviewTypeResponse {
    private Long id;
    private String code;
    private String name;
    private Long activeSchemeId;
}
