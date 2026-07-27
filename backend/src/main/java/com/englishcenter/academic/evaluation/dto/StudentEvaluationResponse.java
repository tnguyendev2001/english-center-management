package com.englishcenter.academic.evaluation.dto;

import com.englishcenter.academic.evaluation.OverallRating;
import com.englishcenter.academic.evaluation.StudentEvaluationStatus;
import java.time.LocalDateTime;

public record StudentEvaluationResponse(
        Long id,
        Long evaluationPeriodId,
        Long classroomId,
        Long studentId,
        Long teacherId,
        String strengths,
        String areasForImprovement,
        String learningAttitude,
        String participation,
        String homeworkPerformance,
        String teacherComment,
        String recommendation,
        String internalNote,
        OverallRating overallRating,
        StudentEvaluationStatus status,
        LocalDateTime publishedAt,
        LocalDateTime finalizedAt,
        String reopenReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
