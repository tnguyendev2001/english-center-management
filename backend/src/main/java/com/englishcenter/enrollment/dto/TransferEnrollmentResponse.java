package com.englishcenter.enrollment.dto;

public record TransferEnrollmentResponse(
        EnrollmentResponse sourceEnrollment,
        EnrollmentResponse targetEnrollment,
        int transferredSessions,
        String warningMessage
) {
}
