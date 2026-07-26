package com.englishcenter.enrollment.dto;

import java.time.LocalDate;

public record ReactivateEnrollmentRequest(
        LocalDate effectiveDate,
        String reason
) {
}
