package com.englishcenter.academic.evaluation.dto;

import com.englishcenter.academic.evaluation.OverallRating;
import jakarta.validation.constraints.NotNull;

public record UpsertStudentEvaluationRequest(
        @NotNull Long evaluationPeriodId,
        @NotNull Long classroomId,
        @NotNull Long studentId,
        String strengths,
        String areasForImprovement,
        String learningAttitude,
        String participation,
        String homeworkPerformance,
        String teacherComment,
        String recommendation,
        String internalNote,
        OverallRating overallRating
) {
}
