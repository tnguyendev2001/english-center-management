package com.englishcenter.academic.assessment.dto;

import com.englishcenter.academic.assessment.AssessmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateAssessmentRequest(
        @NotNull Long classroomId,
        Long classSessionId,
        Long evaluationPeriodId,
        @NotBlank @Size(max = 255) String title,
        String description,
        @NotNull AssessmentType type,
        @NotNull LocalDate assessmentDate,
        @NotNull BigDecimal maxScore,
        BigDecimal weight
) {
}
