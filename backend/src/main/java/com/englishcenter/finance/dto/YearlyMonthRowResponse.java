package com.englishcenter.finance.dto;

import java.math.BigDecimal;

public record YearlyMonthRowResponse(
        int month,
        BigDecimal income,
        BigDecimal expense,
        BigDecimal netCashFlow,
        BigDecimal closingBalance,
        BigDecimal closingStudentDebt
) {
}
