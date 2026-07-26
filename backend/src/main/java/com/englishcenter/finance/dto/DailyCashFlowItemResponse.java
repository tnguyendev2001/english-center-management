package com.englishcenter.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyCashFlowItemResponse(
        LocalDate date,
        BigDecimal income,
        BigDecimal expense,
        BigDecimal netCashFlow
) {
}
