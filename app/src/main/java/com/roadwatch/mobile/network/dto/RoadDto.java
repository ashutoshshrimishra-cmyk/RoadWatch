package com.roadwatch.mobile.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Mirrors backend {@code Road} entity from {@code GET /api/roads}.
 * Used in the Road Intelligence screen.
 */
public class RoadDto {
    public Long id;

    @SerializedName("roadCode")
    public String roadCode;

    public String name;

    @SerializedName("roadType")
    public String roadType;

    @SerializedName("contractorName")
    public String contractorName;

    @SerializedName("lastRelayingDate")
    public String lastRelayingDate;

    @SerializedName("budgetSanctioned")
    public Double budgetSanctioned;

    @SerializedName("budgetSpent")
    public Double budgetSpent;

    public String status;

    /** Budget utilization percentage (0-100). */
    public int getBudgetUtilizationPercent() {
        if (budgetSanctioned == null || budgetSanctioned <= 0) return 0;
        if (budgetSpent == null) return 0;
        return (int) Math.min(100, (budgetSpent / budgetSanctioned) * 100);
    }

    /** Formatted budget string like "12.5L / 20L" */
    public String getBudgetDisplay() {
        return "" + formatLakhs(budgetSpent) + " / " + formatLakhs(budgetSanctioned);
    }

    private String formatLakhs(Double amount) {
        if (amount == null || amount == 0) return "0";
        if (amount >= 10000000) return String.format("%.1fCr", amount / 10000000);
        if (amount >= 100000) return String.format("%.1fL", amount / 100000);
        if (amount >= 1000) return String.format("%.0fK", amount / 1000);
        return String.format("%.0f", amount);
    }
}
