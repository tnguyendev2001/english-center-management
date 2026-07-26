package com.englishcenter.finance.dto;

import com.englishcenter.finance.ChartGrouping;
import com.englishcenter.finance.FinanceScope;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FinanceOverviewResponse(
        FinanceScope scope,
        LocalDate periodStart,
        LocalDate periodEnd,
        LocalDate financeStartDate,
        LocalDate businessDate,
        String minSelectableMonth,
        String maxSelectableMonth,
        LocalDate latestTransactionDate,
        LocalDate latestPaymentDate,
        boolean hasDataBeyondBusinessMonth,
        Integer year,
        Integer month,
        BigDecimal totalBalance,
        BigDecimal cashBalance,
        BigDecimal bankBalance,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal netCashFlow,
        BigDecimal outstandingDebt,
        boolean hasNegativeBalance,
        boolean reconciliationMatched,
        ChartGrouping chartGrouping,
        List<String> warnings,
        List<AccountBalanceResponse> accountBalances,
        List<CategoryBreakdownItemResponse> expenseBreakdown,
        List<CategoryBreakdownItemResponse> incomeBreakdown,
        List<DailyCashFlowItemResponse> incomeExpenseChart,
        List<CashTransactionResponse> recentTransactions,
        List<StudentDebtItemResponse> topDebtStudents
) {
}
