package com.englishcenter.report.dto;

import com.englishcenter.attendance.AttendanceStatus;
import java.time.LocalDate;

public record AttendanceReportItemResponse(
        Long id,
        LocalDate sessionDate,
        String classroomName,
        String studentName,
        AttendanceStatus status,
        String note
) {
}
