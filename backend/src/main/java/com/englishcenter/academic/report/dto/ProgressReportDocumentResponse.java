package com.englishcenter.academic.report.dto;

import com.englishcenter.academic.evaluation.OverallRating;
import com.englishcenter.academic.evaluation.StudentEvaluationStatus;
import com.englishcenter.academic.progress.dto.AcademicProgressSummaryResponse;
import com.englishcenter.academic.progress.dto.AssessmentScoreBreakdownItem;
import com.englishcenter.academic.report.ProgressReportStatus;
import com.englishcenter.center.dto.CenterProfileResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ProgressReportDocumentResponse(
        Long reportId,
        ProgressReportStatus reportStatus,
        LocalDateTime generatedAt,
        LocalDateTime publishedAt,
        CenterProfileResponse centerProfile,
        Long studentId,
        String studentCode,
        String studentName,
        Long classroomId,
        String classCode,
        String className,
        Long teacherId,
        String teacherName,
        Long evaluationPeriodId,
        String evaluationPeriodName,
        LocalDate periodStartDate,
        LocalDate periodEndDate,
        AcademicProgressSummaryResponse progressSummary,
        List<AssessmentScoreBreakdownItem> assessmentBreakdown,
        Long evaluationId,
        StudentEvaluationStatus evaluationStatus,
        String strengths,
        String areasForImprovement,
        String learningAttitude,
        String participation,
        String homeworkPerformance,
        String teacherComment,
        String recommendation,
        OverallRating overallRating
) {
}
