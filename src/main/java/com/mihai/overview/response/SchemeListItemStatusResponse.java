package com.mihai.overview.response;

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
