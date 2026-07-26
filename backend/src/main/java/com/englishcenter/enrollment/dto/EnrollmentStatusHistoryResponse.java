package com.englishcenter.enrollment.dto;

import com.englishcenter.enrollment.EnrollmentStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record EnrollmentStatusHistoryResponse(
        Long id,
        Long enrollmentId,
        EnrollmentStatus status,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        String reason,
        LocalDateTime createdAt
) {
}
