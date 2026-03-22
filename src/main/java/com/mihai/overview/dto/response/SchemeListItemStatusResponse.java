package com.mihai.overview.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class SchemeListItemStatusResponse {
    private Long id;
    private String name;
    private boolean archived;
    private boolean usableForNewReviews;
}
