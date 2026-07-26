package com.englishcenter.enrollment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record TransferEnrollmentRequest(
        @NotNull Long targetClassroomId,
        @NotNull LocalDate targetLearningStartDate,
        @NotBlank String reason
) {
}
