package com.englishcenter.classroom.dto;

import com.englishcenter.classroom.ClassroomStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record ClassroomResponse(
        Long id,
        String classCode,
        String className,
        String level,
        String teacherName,
        String room,
        LocalDate startDate,
        LocalDate expectedEndDate,
        String daysOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        ClassroomStatus status,
        String note,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
