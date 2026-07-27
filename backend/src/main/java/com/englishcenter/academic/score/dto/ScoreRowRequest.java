package com.englishcenter.academic.score.dto;

import com.englishcenter.academic.score.AssessmentScoreStatus;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ScoreRowRequest(
        @NotNull Long studentId,
        BigDecimal score,
        @NotNull AssessmentScoreStatus status,
        String teacherComment
) {
}
