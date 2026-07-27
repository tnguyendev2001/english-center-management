package com.englishcenter.academic.assessment.dto;

import com.englishcenter.academic.assessment.AssessmentStatus;
import com.englishcenter.academic.assessment.AssessmentType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AssessmentResponse(
        Long id,
        Long classroomId,
        Long classSessionId,
        Long evaluationPeriodId,
        String title,
        String description,
        AssessmentType type,
        LocalDate assessmentDate,
        BigDecimal maxScore,
        BigDecimal weight,
        AssessmentStatus status,
        Boolean publishedToStudents,
        Long createdByTeacherId,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
