package com.englishcenter.attendance.dto;

public record AttendanceRosterStudentResponse(
        Long studentId,
        String studentCode,
        String studentName,
        Long enrollmentId
) {
}
