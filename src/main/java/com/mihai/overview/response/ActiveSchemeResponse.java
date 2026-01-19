package com.mihai.overview.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
public class ActiveSchemeResponse {
    private Long schemeId;
    private String schemeName;
    private String reviewTypeCode;
    private List<SchemeItemResponse> items;
}
