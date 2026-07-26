package com.englishcenter.finance.dto;

import java.time.LocalDate;

public record FinanceConfigResponse(
        LocalDate financeStartDate,
        int financeStartYear,
        int financeStartMonth,
        LocalDate businessDate,
        String minSelectableMonth,
        String maxSelectableMonth,
        LocalDate latestTransactionDate,
        LocalDate latestPaymentDate,
        LocalDate latestFinancialDataDate,
        boolean hasDataBeyondBusinessMonth
) {
    /**
     * Backward-compatible alias used by older clients.
     */
    public LocalDate today() {
        return businessDate;
    }
}
