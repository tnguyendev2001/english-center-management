package com.englishcenter.finance.dto;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Authoritative selectable finance period range.
 * maxSelectableMonth can extend past businessDate when existing financial data exists.
 */
public record FinancePeriodRange(
        LocalDate financeStartDate,
        LocalDate businessDate,
        YearMonth minSelectableMonth,
        YearMonth maxSelectableMonth,
        LocalDate latestTransactionDate,
        LocalDate latestPaymentDate,
        LocalDate latestFinancialDataDate
) {
    public boolean hasDataBeyondBusinessMonth() {
        return maxSelectableMonth.isAfter(YearMonth.from(businessDate));
    }

    public String minSelectableMonthLabel() {
        return minSelectableMonth.toString();
    }

    public String maxSelectableMonthLabel() {
        return maxSelectableMonth.toString();
    }
}
