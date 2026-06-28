package com.englishcenter.dashboard.dto;

import com.englishcenter.studentpackage.LearningProgressWarningType;

public record SessionWarningResponse(
        Long studentId,
        String studentName,
        Long classroomId,
        String classroomName,
        Long enrollmentId,
        int totalSessions,
        int usedSessions,
        int remainingSessions,
        LearningProgressWarningType warningType,
        String warningMessage
) {
}
