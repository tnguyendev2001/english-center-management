package com.englishcenter.me.dto;

import com.englishcenter.attendance.AttendanceStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record StudentAttendanceItemResponse(
        Long id,
        Long sessionId,
        LocalDate sessionDate,
        LocalTime startTime,
        LocalTime endTime,
        Long classroomId,
        String classroomName,
        AttendanceStatus status,
        String note,
        LocalDateTime markedAt,
        boolean valid
) {
}
