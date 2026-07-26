package com.englishcenter.finance.dto;

import com.englishcenter.finance.FinancialAccountType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record AccountBalanceResponse(
        Long accountId,
        String accountCode,
        String accountName,
        FinancialAccountType type,
        BigDecimal openingBalance,
        LocalDate openingBalanceDate,
        BigDecimal inflow,
        BigDecimal outflow,
        BigDecimal balance,
        boolean negativeBalance,
        boolean active,
        LocalDate asOfDate
) {
}
