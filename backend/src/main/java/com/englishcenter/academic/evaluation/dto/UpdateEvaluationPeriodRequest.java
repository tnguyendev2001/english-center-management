package com.englishcenter.academic.evaluation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateEvaluationPeriodRequest(
        @NotBlank @Size(max = 200) String name,
        Long classroomId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate
) {
}
