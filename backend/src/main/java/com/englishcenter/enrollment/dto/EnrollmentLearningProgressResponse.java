package com.englishcenter.enrollment.dto;

import com.englishcenter.enrollment.EnrollmentStatus;
import com.englishcenter.studentpackage.LearningProgressWarningType;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Enrollment progress. remainingSessions is totalSessions - usedSessions; makeupAvailableSessions is leave count only. */
public record EnrollmentLearningProgressResponse(
        Long enrollmentId,
        EnrollmentStatus status,
        LocalDate startDate,
        LocalDate endDate,
        Long studentId,
        String studentCode,
        String studentName,
        Long classroomId,
        String classroomName,
        int totalSessions,
        int usedSessions,
        int remainingSessions,
        int overusedSessions,
        Long latestStudentPackageId,
        String latestPackageName,
        BigDecimal latestPackagePrice,
        Integer latestPackageTotalSessions,
        Long latestTuitionPackageId,
        int makeupAvailableSessions,
        LearningProgressWarningType warningType,
        String warningMessage
) {
}
