package com.englishcenter.attendance.dto;

import java.util.List;

public record AttendanceReadinessResponse(
        Long sessionId,
        Long classroomId,
        boolean ready,
        List<AttendanceReadinessBlockedStudentResponse> blockedStudents,
        List<AttendanceReadinessActivatedPackageResponse> activatedPackages
) {
}
