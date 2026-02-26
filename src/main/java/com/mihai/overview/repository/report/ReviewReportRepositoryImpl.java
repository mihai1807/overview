package com.mihai.overview.repository.report;

import com.mihai.overview.entity.ReviewStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class ReviewReportRepositoryImpl implements ReviewReportRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public ReviewAverageAgg aggregateAverageFinalScores(
            LocalDateTime fromInclusive,
            LocalDateTime toExclusive,
            List<Long> interactionTypeIds,
            List<Long> reviewedUserIdsOrNull
    ) {
        if (interactionTypeIds == null || interactionTypeIds.isEmpty()) {
            // service guarantees non-empty by using all unarchived types when omitted
            return new ReviewAverageAgg(0, 0);
        }

        StringBuilder jpql = new StringBuilder("""
            select
              count(r.id),
              coalesce(sum(r.totalScore), 0)
            from Review r
            where r.status = :status
              and r.occurredAt >= :from
              and r.occurredAt < :to
              and r.interactionType.id in :typeIds
        """);

        if (reviewedUserIdsOrNull != null && !reviewedUserIdsOrNull.isEmpty()) {
            jpql.append(" and r.reviewedUserId in :userIds ");
        }

        TypedQuery<Object[]> q = em.createQuery(jpql.toString(), Object[].class);
        q.setParameter("status", ReviewStatus.FINAL);
        q.setParameter("from", fromInclusive);
        q.setParameter("to", toExclusive);
        q.setParameter("typeIds", interactionTypeIds);

        if (reviewedUserIdsOrNull != null && !reviewedUserIdsOrNull.isEmpty()) {
            q.setParameter("userIds", reviewedUserIdsOrNull);
        }

        Object[] row = q.getSingleResult();
        long count = (Long) row[0];
        long sum = ((Number) row[1]).longValue();

        return new ReviewAverageAgg(count, sum);
    }
}