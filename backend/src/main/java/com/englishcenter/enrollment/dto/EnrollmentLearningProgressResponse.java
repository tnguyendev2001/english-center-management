package com.englishcenter.enrollment.dto;

import com.englishcenter.studentpackage.LearningProgressWarningType;
import java.math.BigDecimal;

public record EnrollmentLearningProgressResponse(
        Long enrollmentId,
        Long studentId,
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
