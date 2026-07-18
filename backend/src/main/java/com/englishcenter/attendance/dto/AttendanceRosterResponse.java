package com.englishcenter.attendance.dto;

import java.util.List;

public record AttendanceRosterResponse(
        Long sessionId,
        List<AttendanceRosterStudentResponse> students
) {
}
