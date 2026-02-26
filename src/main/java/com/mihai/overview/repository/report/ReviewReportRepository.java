package com.mihai.overview.repository.report;

import java.time.LocalDateTime;
import java.util.List;

public interface ReviewReportRepository {

    ReviewAverageAgg aggregateAverageFinalScores(
            LocalDateTime fromInclusive,
            LocalDateTime toExclusive,
            List<Long> interactionTypeIds,
            List<Long> reviewedUserIdsOrNull // null => no agent filter
    );
}