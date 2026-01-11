package com.mihai.overview.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ReviewRequest {

    @NotNull(message = "Ticket ID is mandatory")
    @Min(value = 1000000, message = "Ticket ID has to be 7 digits long")
    @Max(value = 9999999, message = "Ticket ID has to be 7 digits long")
    private Long ticketId;

    @NotBlank(message = "Review type is mandatory")
    private String reviewType;

    @NotNull(message = "Interaction date is mandatory")
    private LocalDate interactionDate;

    @NotNull(message = "Interaction time is mandatory")
    private LocalTime interactionTime;

    @NotNull(message = "CID is mandatory")
    @Min(value = 0, message = "CID must be between 0000000000 and 9999999999")
    @Max(value = 9999999999L, message = "CID must be between 0000000000 and 9999999999")
    private Long cid;

    @NotNull(message = "Reviewed user is mandatory")
    private Long reviewedUserId;

    @Min(value = 0, message = "Final Score min 0")
    @Max(value = 100, message = "Final Score max 100")
    private int interactionScore;
}
