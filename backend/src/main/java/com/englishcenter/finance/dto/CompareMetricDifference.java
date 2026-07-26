package com.englishcenter.finance.dto;

import com.englishcenter.finance.ComparisonTrend;
import java.math.BigDecimal;

public record CompareMetricDifference(
        BigDecimal amount,
        BigDecimal percentage,
        ComparisonTrend trend,
        boolean percentageUnavailable
) {
}
