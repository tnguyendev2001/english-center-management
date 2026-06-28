package com.englishcenter.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RevenueByDateItem(
        LocalDate date,
        BigDecimal amount
) {
}
