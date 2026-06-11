package com.roadwatch.mobile.network.dto;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * DTO matching the backend Budget entity.
 *
 * Backend fields (BigDecimal serialised as plain number by Jackson):
 *   id, roadType, roadName, contractorName,
 *   amountSanctioned, amountSpent, lastRelayingDate
 *
 * Gson deserialises JSON numbers into String fields fine, but we
 * expose typed helpers so the UI never has to parse strings itself.
 */
public class BudgetDto {

    public Long   id;

    @SerializedName("roadType")
    public String roadType;          // NH | SH | MDR

    @SerializedName("roadName")
    public String roadName;

    @SerializedName("contractorName")
    public String contractorName;

    @SerializedName("amountSanctioned")
    public String amountSanctioned;  // e.g. "50000000.00"

    @SerializedName("amountSpent")
    public String amountSpent;       // e.g. "12000000.00"

    @SerializedName("lastRelayingDate")
    public String lastRelayingDate;  // e.g. "2023-06-15"

    // ”€”€ Typed helpers ”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€

    /** Returns amountSanctioned as double, 0 if null/unparseable. */
    public double sanctionedDouble() {
        return parseDouble(amountSanctioned);
    }

    /** Returns amountSpent as double, 0 if null/unparseable. */
    public double spentDouble() {
        return parseDouble(amountSpent);
    }

    /**
     * Spend percentage: (spent / sanctioned) * 100, clamped to [0, 100].
     * Returns 0 if sanctioned is zero.
     */
    public int spendPercent() {
        double s = sanctionedDouble();
        if (s <= 0) return 0;
        double pct = (spentDouble() / s) * 100.0;
        return (int) Math.min(100, Math.max(0, pct));
    }

    /**
     * True when spending has reached or exceeded 90 % of the budget.
     * Used to colour the card red.
     */
    public boolean isOverBudget() {
        return spendPercent() >= 90;
    }

    /** Formats a raw decimal string as " X.XX Cr" or " X.XX L". */
    public static String formatAmount(String raw) {
        double v = parseDouble(raw);
        if (v == 0) return " 0";
        if (v >= 1_00_00_000) {                          //  1 Crore
            return String.format(" %.2f Cr", v / 1_00_00_000.0);
        } else if (v >= 1_00_000) {                      //  1 Lakh
            return String.format(" %.2f L", v / 1_00_000.0);
        } else {
            return String.format(" %.0f", v);
        }
    }

    private static double parseDouble(String s) {
        if (s == null || s.isEmpty()) return 0;
        try { return Double.parseDouble(s); }
        catch (NumberFormatException e) { return 0; }
    }
}
