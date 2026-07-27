package com.englishcenter.me.dto;

import com.englishcenter.classroom.ClassDayOfWeek;
import com.englishcenter.enrollment.EnrollmentStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record StudentClassItemResponse(
        Long enrollmentId,
        Long classroomId,
        String classCode,
        String className,
        String level,
        String teacherName,
        String room,
        List<ClassDayOfWeek> daysOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        LocalDate learningStartDate,
        EnrollmentStatus enrollmentStatus,
        Integer totalSessions,
        Integer usedSessions,
        Integer remainingSessions
) {
}
