package com.englishcenter.enrollment.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record HoldEnrollmentRequest(
        LocalDate effectiveDate,
        LocalDate expectedReturnDate,
        @NotBlank String reason
) {
}
