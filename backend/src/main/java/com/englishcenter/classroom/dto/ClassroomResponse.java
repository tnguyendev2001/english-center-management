package com.englishcenter.classroom.dto;

import com.englishcenter.classroom.ClassDayOfWeek;
import com.englishcenter.classroom.ClassroomStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record ClassroomResponse(
        Long id,
        String classCode,
        String className,
        String level,
        String teacherName,
        String room,
        LocalDate startDate,
        LocalDate expectedEndDate,
        List<ClassDayOfWeek> daysOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        ClassroomStatus status,
        String note,
        int studentsOverusedSessionsCount,
        int studentsOutOfSessionsCount,
        int studentsLowSessionsCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
