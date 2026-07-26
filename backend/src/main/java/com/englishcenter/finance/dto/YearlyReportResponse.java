package com.englishcenter.finance.dto;

import java.math.BigDecimal;
import java.util.List;

public record YearlyReportResponse(
        int year,
        List<YearlyMonthRowResponse> months,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal annualNetCashFlow,
        Integer highestIncomeMonth,
        Integer highestExpenseMonth,
        String largestExpenseCategoryCode,
        String largestExpenseCategoryName,
        BigDecimal averageMonthlyIncome,
        BigDecimal averageMonthlyExpense,
        BigDecimal yearEndBalance,
        BigDecimal yearEndStudentDebt,
        BigDecimal incomeChangeVsPreviousYear,
        BigDecimal expenseChangeVsPreviousYear,
        BigDecimal netCashFlowChangeVsPreviousYear,
        List<CategoryBreakdownItemResponse> expenseBreakdown
) {
}
