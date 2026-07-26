package com.englishcenter.finance.dto;

import com.englishcenter.finance.FinancialAccountType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record FinancialAccountResponse(
        Long id,
        String code,
        String name,
        FinancialAccountType type,
        BigDecimal openingBalance,
        LocalDate openingBalanceDate,
        BigDecimal currentBalance,
        boolean negativeBalance,
        boolean active,
        String note,
        int displayOrder,
        LocalDate lastTransactionDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
