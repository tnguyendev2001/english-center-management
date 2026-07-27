package com.englishcenter.academic.progress.dto;

import com.englishcenter.academic.assessment.AssessmentType;
import com.englishcenter.academic.score.AssessmentScoreStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

public record AssessmentScoreBreakdownItem(
        Long assessmentId,
        String title,
        LocalDate assessmentDate,
        AssessmentType type,
        BigDecimal maxScore,
        BigDecimal score,
        AssessmentScoreStatus status,
        BigDecimal normalizedPercentage,
        BigDecimal weight
) {
}
