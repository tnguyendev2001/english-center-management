package com.englishcenter.dashboard.dto;

import java.math.BigDecimal;

public record DashboardOverviewSummaryResponse(
        Long activeStudents,
        Long activeClassrooms,
        Long todayClasses,
        BigDecimal monthlyRevenue,
        BigDecimal currentDebt,
        Long studentsWithDebt,
        Long studentsOutOfSessions,
        Long studentsNearlyOutOfSessions,
        Long assignedClassrooms,
        Long incompleteAttendance,
        Integer remainingSessions,
        BigDecimal outstandingDebt
) {
}
