package com.englishcenter.studentpackage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record AdjustStudentPackagePeriodStartRequest(
        @NotNull(message = "Period start date is required")
        LocalDate periodStartDate,

        @NotBlank(message = "Adjustment reason is required")
        @Size(max = 1000, message = "Adjustment reason must not exceed 1000 characters")
        String reason
) {
}
