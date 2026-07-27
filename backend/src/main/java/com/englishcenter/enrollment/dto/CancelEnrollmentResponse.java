package com.englishcenter.enrollment.dto;

import com.englishcenter.enrollment.EnrollmentStatus;
import java.util.List;

public record CancelEnrollmentResponse(
        Long enrollmentId,
        EnrollmentStatus status,
        List<Long> canceledInvoiceIds,
        int canceledInvoiceCount,
        EnrollmentResponse enrollment
) {
}
