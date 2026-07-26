package com.englishcenter.enrollment.dto;

import com.englishcenter.enrollment.EnrollmentStatus;
import java.util.List;

public record DuplicateEnrollmentGroupResponse(
        Long studentId,
        Long classroomId,
        List<Long> enrollmentIds,
        List<EnrollmentStatus> statuses,
        long attendanceCount,
        long invoiceCount,
        long validPaymentCount,
        boolean requiresManualCleanup
) {
}
