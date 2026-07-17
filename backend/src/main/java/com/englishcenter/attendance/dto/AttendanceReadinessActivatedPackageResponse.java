package com.englishcenter.attendance.dto;

public record AttendanceReadinessActivatedPackageResponse(
        Long studentId,
        Long enrollmentId,
        String studentCode,
        String studentName,
        Long activatedStudentPackageId,
        String packageName
) {
}
