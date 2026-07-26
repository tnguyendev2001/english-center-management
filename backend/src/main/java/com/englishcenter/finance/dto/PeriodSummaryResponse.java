package com.englishcenter.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PeriodSummaryResponse(
        int year,
        int month,
        LocalDate fromDate,
        LocalDate toDate,
        String periodStatus,
        BigDecimal openingBalance,
        BigDecimal totalIncome,
        BigDecimal tuitionIncome,
        BigDecimal otherIncome,
        BigDecimal totalExpense,
        BigDecimal netCashFlow,
        BigDecimal closingBalance,
        boolean formulaReconciles,
        BigDecimal openingDebt,
        BigDecimal newInvoicesAmount,
        BigDecimal paymentsCollected,
        BigDecimal closingDebt,
        boolean fromSnapshot,
        List<String> warnings
) {
}
