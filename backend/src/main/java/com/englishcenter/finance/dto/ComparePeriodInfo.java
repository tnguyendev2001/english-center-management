package com.englishcenter.finance.dto;

public record ComparePeriodInfo(
        int year,
        int month,
        String label,
        boolean hasData,
        boolean hasTransactions,
        boolean hasFinancialState
) {
}
