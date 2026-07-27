package com.englishcenter.academic.score.dto;

import com.englishcenter.academic.score.AssessmentScoreStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AssessmentScoreResponse(
        Long id,
        Long assessmentId,
        Long studentId,
        BigDecimal score,
        AssessmentScoreStatus status,
        String teacherComment,
        String gradedBy,
        LocalDateTime gradedAt,
        LocalDateTime publishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
