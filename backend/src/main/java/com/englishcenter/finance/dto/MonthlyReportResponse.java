package com.englishcenter.finance.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record MonthlyReportResponse(
        PeriodSummaryResponse summary,
        List<CategoryBreakdownItemResponse> expenseBreakdown,
        List<CategoryBreakdownItemResponse> incomeBreakdown,
        List<AccountBalanceResponse> accountBalances,
        List<DailyCashFlowItemResponse> dailyCashFlow,
        List<CashTransactionResponse> transactions,
        Map<String, BigDecimal> expenseByCode
) {
}
