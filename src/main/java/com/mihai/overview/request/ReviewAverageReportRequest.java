package com.mihai.overview.request;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public class ReviewAverageReportRequest {

    /**
     * Either month (YYYY-MM) OR (from + to).
     * If both provided -> 400. If neither -> 400.
     */
    private String month; // "2026-01" (validated in service)

    private LocalDate from; // inclusive
    private LocalDate to;   // inclusive

    @Size(max = 5000)
    private List<Long> agentIds; // optional; empty/null => all enabled users

    @Size(max = 100)
    private List<String> interactionTypeCodes; // optional; empty/null => all unarchived types

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public LocalDate getFrom() {
        return from;
    }

    public void setFrom(LocalDate from) {
        this.from = from;
    }

    public LocalDate getTo() {
        return to;
    }

    public void setTo(LocalDate to) {
        this.to = to;
    }

    public List<Long> getAgentIds() {
        return agentIds;
    }

    public void setAgentIds(List<Long> agentIds) {
        this.agentIds = agentIds;
    }

    public List<String> getInteractionTypeCodes() {
        return interactionTypeCodes;
    }

    public void setInteractionTypeCodes(List<String> interactionTypeCodes) {
        this.interactionTypeCodes = interactionTypeCodes;
    }
}