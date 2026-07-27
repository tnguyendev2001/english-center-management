package com.englishcenter.academic.evaluation.dto;

import com.englishcenter.academic.evaluation.EvaluationPeriodStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record EvaluationPeriodResponse(
        Long id,
        String name,
        Long classroomId,
        LocalDate startDate,
        LocalDate endDate,
        EvaluationPeriodStatus status,
        String reopenReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
