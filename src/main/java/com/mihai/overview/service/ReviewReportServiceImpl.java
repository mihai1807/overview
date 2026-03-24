package com.mihai.overview.service;

import com.mihai.overview.entity.InteractionType;
import com.mihai.overview.entity.User;
import com.mihai.overview.exception.BadRequestException;
import com.mihai.overview.repository.InteractionTypeRepository;
import com.mihai.overview.repository.UserRepository;
import com.mihai.overview.repository.report.ReviewAverageAgg;
import com.mihai.overview.repository.report.ReviewReportRepository;
import com.mihai.overview.dto.request.ReviewAverageReportRequest;
import com.mihai.overview.dto.response.ReportWarningResponse;
import com.mihai.overview.dto.response.ReviewAverageReportResponse;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ReviewReportServiceImpl implements ReviewReportService {

    private final ReviewReportRepository reviewReportRepository;
    private final InteractionTypeRepository interactionTypeRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public ReviewAverageReportResponse getAverageScore(ReviewAverageReportRequest request) {

        if (request == null) {
            throw new BadRequestException("Request body is mandatory");
        }

        DateRange range = resolveDateRangeOrThrow(request);

        // --- validate & resolve filters (collect all problems first) ---
        ValidationIssues issues = new ValidationIssues();
        List<ReportWarningResponse> warnings = new ArrayList<>();

        // interaction types
        List<Long> interactionTypeIds = resolveInteractionTypeIdsOrCollectIssues(
                request.getInteractionTypeCodes(),
                issues,
                warnings
        );

        // agents
        List<Long> agentIdsOrNull = resolveAgentIdsOrCollectIssues(
                request.getAgentIds(),
                issues,
                warnings
        );

        if (issues.hasAny()) {
            throw new BadRequestException(issues.toCombinedMessage());
        }

        // --- aggregate ---
        ReviewAverageAgg agg = reviewReportRepository.aggregateAverageFinalScores(
                range.fromInclusive(),
                range.toExclusive(),
                interactionTypeIds,
                agentIdsOrNull
        );

        long count = agg.getReviewCount();
        long sum = agg.getSumScore();

        double avg = 0.00;
        if (count > 0) {
            avg = BigDecimal.valueOf((double) sum / (double) count)
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();
        } else {
            warnings.add(new ReportWarningResponse(
                    "NO_REVIEWS",
                    buildNoReviewsMessage(request, range)
            ));
        }

        return new ReviewAverageReportResponse(count, sum, avg, warnings);
    }

    private DateRange resolveDateRangeOrThrow(ReviewAverageReportRequest req) {
        boolean hasMonth = req.getMonth() != null && !req.getMonth().trim().isEmpty();
        boolean hasFromTo = req.getFrom() != null || req.getTo() != null;

        if (hasMonth && hasFromTo) {
            throw new BadRequestException("Provide either month OR (from and to), not both.");
        }
        if (!hasMonth && !hasFromTo) {
            throw new BadRequestException("Provide either month OR (from and to).");
        }

        LocalDate from;
        LocalDate to;

        if (hasMonth) {
            String raw = req.getMonth().trim();
            YearMonth ym;
            try {
                ym = YearMonth.parse(raw);
            } catch (DateTimeParseException e) {
                throw new BadRequestException("Invalid month format. Expected YYYY-MM.");
            }
            from = ym.atDay(1);
            to = ym.atEndOfMonth();
        } else {
            if (req.getFrom() == null || req.getTo() == null) {
                throw new BadRequestException("Both from and to are required when month is not provided.");
            }
            from = req.getFrom();
            to = req.getTo();
            if (from.isAfter(to)) {
                throw new BadRequestException("from must be <= to.");
            }
        }

        LocalDateTime fromInclusive = from.atStartOfDay();
        LocalDateTime toExclusive = to.plusDays(1).atStartOfDay();

        return new DateRange(fromInclusive, toExclusive, from, to);
    }

    private List<Long> resolveInteractionTypeIdsOrCollectIssues(
            List<String> codesRaw,
            ValidationIssues issues,
            List<ReportWarningResponse> warnings
    ) {
        List<String> codes = normalizeCodes(codesRaw);

        if (codes.isEmpty()) {
            // use all unarchived types
            List<InteractionType> all = interactionTypeRepository.findAllByArchivedFalseOrderByCodeAsc();
            List<Long> ids = all.stream().map(InteractionType::getId).toList();

            warnings.add(new ReportWarningResponse(
                    "INTERACTION_TYPES_ALL_UNARCHIVED",
                    "interactionTypeCodes not provided; using all unarchived interaction types."
            ));

            // If there are zero unarchived types, the aggregate will return 0; keep it as-is.
            return ids;
        }

        // fetch all interaction types matching the requested codes
        List<InteractionType> found = interactionTypeRepository.findByCodeIn(codes);

        Set<String> foundCodes = found.stream()
                .map(it -> it.getCode().toUpperCase())
                .collect(Collectors.toSet());

        List<String> unknown = codes.stream()
                .filter(c -> !foundCodes.contains(c))
                .toList();

        if (!unknown.isEmpty()) {
            issues.unknownInteractionTypeCodes.addAll(unknown);
        }

        List<String> archived = found.stream()
                .filter(InteractionType::isArchived)
                .map(InteractionType::getCode)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        if (!archived.isEmpty()) {
            issues.archivedInteractionTypeCodes.addAll(archived);
        }

        // only valid if none unknown and none archived
        return found.stream()
                .filter(it -> !it.isArchived())
                .map(InteractionType::getId)
                .toList();
    }

    private List<Long> resolveAgentIdsOrCollectIssues(
            List<Long> agentIdsRaw,
            ValidationIssues issues,
            List<ReportWarningResponse> warnings
    ) {
        List<Long> ids = normalizeIds(agentIdsRaw);

        if (ids.isEmpty()) {
            warnings.add(new ReportWarningResponse(
                    "AGENTS_ALL_ENABLED",
                    "agentIds not provided; using all enabled users (agent filter not applied)."
            ));
            // no agent filter
            return null;
        }

        // CrudRepository has findAllById
        Iterable<User> usersIt = userRepository.findAllById(ids);
        List<User> users = new ArrayList<>();
        usersIt.forEach(users::add);

        Set<Long> foundIds = users.stream()
                .map(User::getId)
                .collect(Collectors.toSet());

        List<Long> invalid = ids.stream()
                .filter(id -> !foundIds.contains(id))
                .toList();

        if (!invalid.isEmpty()) {
            issues.invalidUserIds.addAll(invalid);
        }

        List<Long> disabled = users.stream()
                .filter(u -> !u.isEnabled())
                .map(User::getId)
                .sorted()
                .toList();

        if (!disabled.isEmpty()) {
            issues.disabledUserIds.addAll(disabled);
        }

        // only enabled users pass to query
        return users.stream()
                .filter(User::isEnabled)
                .map(User::getId)
                .toList();
    }

    private static List<String> normalizeCodes(List<String> raw) {
        if (raw == null) return List.of();
        return raw.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toUpperCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    private static List<Long> normalizeIds(List<Long> raw) {
        if (raw == null) return List.of();
        return raw.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .toList();
    }

    private String buildNoReviewsMessage(ReviewAverageReportRequest req, DateRange range) {
        String datePart = range.fromDate() + " to " + range.toDate();

        String agentsPart;
        if (req.getAgentIds() == null || req.getAgentIds().isEmpty()) {
            agentsPart = "all enabled users";
        } else {
            // names not resolved in message for v1.0; you still get warnings + caller knows their selection.
            agentsPart = "selected users";
        }

        String typesPart;
        if (req.getInteractionTypeCodes() == null || req.getInteractionTypeCodes().isEmpty()) {
            typesPart = "all unarchived interaction types";
        } else {
            typesPart = "selected interaction types";
        }

        return "No FINAL reviews found for " + agentsPart + " and " + typesPart + " in " + datePart + ".";
    }

    private record DateRange(LocalDateTime fromInclusive, LocalDateTime toExclusive, LocalDate fromDate, LocalDate toDate) {}

    private static class ValidationIssues {
        private final List<String> unknownInteractionTypeCodes = new ArrayList<>();
        private final List<String> archivedInteractionTypeCodes = new ArrayList<>();
        private final List<Long> invalidUserIds = new ArrayList<>();
        private final List<Long> disabledUserIds = new ArrayList<>();

        boolean hasAny() {
            return !unknownInteractionTypeCodes.isEmpty()
                    || !archivedInteractionTypeCodes.isEmpty()
                    || !invalidUserIds.isEmpty()
                    || !disabledUserIds.isEmpty();
        }

        String toCombinedMessage() {
            // Only include non-empty sections.
            List<String> parts = new ArrayList<>();

            if (!unknownInteractionTypeCodes.isEmpty()) {
                parts.add("unknownInteractionTypeCodes: " + unknownInteractionTypeCodes);
            }
            if (!archivedInteractionTypeCodes.isEmpty()) {
                parts.add("archivedInteractionTypeCodes: " + archivedInteractionTypeCodes);
            }
            if (!invalidUserIds.isEmpty()) {
                parts.add("invalidUserIds: " + invalidUserIds);
            }
            if (!disabledUserIds.isEmpty()) {
                parts.add("disabledUserIds: " + disabledUserIds);
            }

            return String.join(". ", parts);
        }
    }
}