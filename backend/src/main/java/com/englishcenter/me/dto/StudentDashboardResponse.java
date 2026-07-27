package com.englishcenter.me.dto;

import com.englishcenter.enrollment.dto.EnrollmentResponse;
import com.englishcenter.invoice.dto.InvoiceResponse;
import java.math.BigDecimal;
import java.util.List;

public record StudentDashboardResponse(
        int activeClassCount,
        StudentScheduleItemResponse nextSession,
        int usedSessions,
        int remainingSessions,
        BigDecimal totalOutstandingDebt,
        List<StudentScheduleItemResponse> upcomingSessions,
        List<EnrollmentResponse> progressItems,
        List<StudentAttendanceItemResponse> recentAttendance,
        List<InvoiceResponse> attentionInvoices
) {
}
