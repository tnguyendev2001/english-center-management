package com.englishcenter.report.dto;

import com.englishcenter.classroom.ClassroomStatus;
import java.math.BigDecimal;

public record ClassroomReportItemResponse(
        Long classroomId,
        String classroomName,
        ClassroomStatus classroomStatus,
        long activeStudentCount,
        long depletedStudentCount,
        long lowSessionStudentCount,
        BigDecimal totalDebtAmount,
        BigDecimal revenueThisMonth,
        int upcomingSessionCount
) {
}
