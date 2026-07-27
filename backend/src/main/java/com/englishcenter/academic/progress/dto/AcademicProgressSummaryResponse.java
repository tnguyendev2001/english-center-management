package com.englishcenter.academic.progress.dto;

import java.math.BigDecimal;

public record AcademicProgressSummaryResponse(
        Long studentId,
        Long classroomId,
        Long evaluationPeriodId,
        int sessionsHeld,
        int sessionsPresent,
        int sessionsAbsent,
        int sessionsExcused,
        BigDecimal attendanceRate,
        int assignmentsAssigned,
        int assignmentsSubmitted,
        int assignmentsLate,
        int assignmentsGraded,
        BigDecimal assignmentCompletionRate,
        int assessmentCount,
        BigDecimal averageAssessmentPercentage,
        BigDecimal weightedAssessmentAverage,
        Integer currentEnrollmentTotalSessions,
        Integer currentEnrollmentUsedSessions,
        Integer currentEnrollmentRemainingSessions
) {
}
