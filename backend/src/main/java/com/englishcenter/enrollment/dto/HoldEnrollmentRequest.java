package com.englishcenter.enrollment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record HoldEnrollmentRequest(
        @NotNull(message = "Vui lòng chọn ngày bắt đầu nghỉ") LocalDate effectiveDate,
        LocalDate expectedReturnDate,
        @NotBlank String reason
) {
}
