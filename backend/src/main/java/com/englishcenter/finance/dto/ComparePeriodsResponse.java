package com.englishcenter.finance.dto;

public record ComparePeriodsResponse(
        ComparePeriodInfo currentPeriod,
        ComparePeriodInfo comparisonPeriod,
        ComparePeriodMetrics current,
        ComparePeriodMetrics comparison,
        CompareMetricDifference income,
        CompareMetricDifference expense,
        CompareMetricDifference netCashFlow,
        CompareMetricDifference closingBalance,
        CompareMetricDifference outstandingDebt,
        String message,
        String suggestion,
        Integer nearestEarlierYearWithData,
        Integer nearestEarlierMonthWithData
) {
}
