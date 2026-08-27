package com.englishcenter.enrollment.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ChangeLearningStartDateRequest(
        @NotNull(message = "Vui lòng chọn ngày bắt đầu học") LocalDate learningStartDate,
        String reason
) {
}
