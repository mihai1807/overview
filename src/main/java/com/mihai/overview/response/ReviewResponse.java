package com.mihai.overview.response;

import com.mihai.overview.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class ReviewResponse {

    private Long id;

    private Long ticketId;

    private Long reviewedUserId;

    private int interactionScore;


}
