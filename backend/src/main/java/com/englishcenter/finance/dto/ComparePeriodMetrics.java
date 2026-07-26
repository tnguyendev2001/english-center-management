package com.englishcenter.finance.dto;

import java.math.BigDecimal;

public record ComparePeriodMetrics(
        BigDecimal income,
        BigDecimal expense,
        BigDecimal netCashFlow,
        BigDecimal closingBalance,
        BigDecimal outstandingDebt
) {
}
