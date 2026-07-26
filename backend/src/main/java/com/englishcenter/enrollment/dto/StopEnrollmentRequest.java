package com.englishcenter.enrollment.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record StopEnrollmentRequest(
        LocalDate effectiveDate,
        @NotBlank String reason
) {
}
