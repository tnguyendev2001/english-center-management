package com.englishcenter.finance.dto;

import java.util.List;

public record CompareDefaultsResponse(
        int currentYear,
        int currentMonth,
        int comparisonYear,
        int comparisonMonth,
        List<String> monthsWithData
) {
}
