package com.englishcenter.revenue.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RevenueSummaryResponse(
        LocalDate today,
        BigDecimal todayRevenue,
        BigDecimal monthRevenue,
        BigDecimal totalRevenue
) {
}
