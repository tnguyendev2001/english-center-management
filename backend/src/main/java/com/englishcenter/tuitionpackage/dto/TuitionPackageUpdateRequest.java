package com.englishcenter.tuitionpackage.dto;

import com.englishcenter.tuitionpackage.TuitionPackageStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record TuitionPackageUpdateRequest(
        @NotBlank(message = "Package name is required")
        @Size(max = 255, message = "Package name must not exceed 255 characters")
        String name,

        @Positive(message = "Sessions per week must be greater than 0")
        Integer sessionsPerWeek,

        @NotNull(message = "Total sessions is required")
        @Positive(message = "Total sessions must be greater than 0")
        Integer totalSessions,

        @Positive(message = "Expected months must be greater than 0")
        Integer expectedMonths,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.00", inclusive = false, message = "Price must be greater than 0")
        BigDecimal price,

        @NotNull(message = "Status is required")
        TuitionPackageStatus status
) {
}
