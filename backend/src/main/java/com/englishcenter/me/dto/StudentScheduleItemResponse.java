package com.englishcenter.me.dto;

import com.englishcenter.classsession.ClassSessionStatus;
import java.time.LocalDate;
import java.time.LocalTime;

public record StudentScheduleItemResponse(
        Long sessionId,
        Long classroomId,
        String classroomName,
        LocalDate sessionDate,
        LocalTime startTime,
        LocalTime endTime,
        String room,
        ClassSessionStatus status
) {
}
