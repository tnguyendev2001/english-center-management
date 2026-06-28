package com.englishcenter.dashboard.dto;

import java.math.BigDecimal;

public record DashboardSummaryResponse(
        long totalActiveStudents,
        long totalActiveClassrooms,
        long totalActiveEnrollments,
        long totalStudentsWithDepletedSessions,
        long totalStudentsWithLowSessions,
        long totalStudentsWithDebt,
        long totalUnpaidInvoices,
        long totalPartiallyPaidInvoices,
        BigDecimal totalDebtAmount,
        BigDecimal totalRevenueToday,
        BigDecimal totalRevenueThisMonth,
        BigDecimal totalRevenueThisYear,
        int totalPendingMakeupCredits,
        int upcomingSessionsToday,
        int completedSessionsThisMonth
) {
}
