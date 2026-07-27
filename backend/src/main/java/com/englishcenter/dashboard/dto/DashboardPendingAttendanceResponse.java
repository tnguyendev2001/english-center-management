package com.englishcenter.dashboard.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record DashboardPendingAttendanceResponse(
        Long sessionId,
        Long classroomId,
        String classroomName,
        String teacherName,
        LocalDate sessionDate,
        LocalTime startTime,
        LocalTime endTime,
        int eligibleStudentCount,
        int markedCount,
        int missingCount
) {
}
