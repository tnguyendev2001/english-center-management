package com.englishcenter.attendance.dto;

public record AttendanceReadinessBlockedStudentResponse(
        Long studentId,
        Long enrollmentId,
        String studentName,
        int remainingSessions,
        String reason
) {
}
