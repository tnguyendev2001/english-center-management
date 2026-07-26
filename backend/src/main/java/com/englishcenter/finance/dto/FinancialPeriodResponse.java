package com.englishcenter.finance.dto;

import com.englishcenter.finance.FinancialPeriodStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FinancialPeriodResponse(
        Long id,
        int year,
        int month,
        FinancialPeriodStatus status,
        LocalDateTime closedAt,
        String closedBy,
        LocalDateTime reopenedAt,
        String reopenedBy,
        String reopenReason,
        String note,
        BigDecimal openingBalanceSnapshot,
        BigDecimal totalIncomeSnapshot,
        BigDecimal totalExpenseSnapshot,
        BigDecimal closingBalanceSnapshot,
        BigDecimal outstandingDebtSnapshot,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
