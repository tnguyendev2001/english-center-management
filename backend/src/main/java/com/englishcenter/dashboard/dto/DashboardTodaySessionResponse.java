package com.englishcenter.dashboard.dto;

import com.englishcenter.dashboard.DashboardSessionAttendanceStatus;
import java.time.LocalDate;
import java.time.LocalTime;

public record DashboardTodaySessionResponse(
        Long sessionId,
        Long classroomId,
        String classroomName,
        String teacherName,
        String room,
        LocalDate sessionDate,
        LocalTime startTime,
        LocalTime endTime,
        int activeStudentCount,
        DashboardSessionAttendanceStatus attendanceStatus
) {
}
