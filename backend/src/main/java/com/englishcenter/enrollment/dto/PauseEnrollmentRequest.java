package com.englishcenter.enrollment.dto;

import com.englishcenter.enrollment.EnrollmentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record PauseEnrollmentRequest(
        @NotNull(message = "Vui lòng chọn trạng thái") EnrollmentStatus status,
        @NotNull(message = "Vui lòng chọn ngày bắt đầu nghỉ") LocalDate effectiveDate,
        LocalDate expectedReturnDate,
        @NotBlank String reason
) {
}
