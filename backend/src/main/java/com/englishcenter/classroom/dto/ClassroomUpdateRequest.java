package com.englishcenter.classroom.dto;

import com.englishcenter.classroom.ClassDayOfWeek;
import com.englishcenter.classroom.ClassroomStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

public record ClassroomUpdateRequest(
        @NotBlank(message = "Class code is required")
        @Size(max = 50, message = "Class code must not exceed 50 characters")
        String classCode,

        @NotBlank(message = "Class name is required")
        @Size(max = 255, message = "Class name must not exceed 255 characters")
        String className,

        @NotBlank(message = "Level is required")
        @Size(max = 100, message = "Level must not exceed 100 characters")
        String level,

        @NotBlank(message = "Teacher name is required")
        @Size(max = 255, message = "Teacher name must not exceed 255 characters")
        String teacherName,

        @Size(max = 100, message = "Room must not exceed 100 characters")
        String room,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        LocalDate expectedEndDate,

        @NotEmpty(message = "Days of week is required")
        Set<ClassDayOfWeek> daysOfWeek,

        @NotNull(message = "Start time is required")
        LocalTime startTime,

        @NotNull(message = "End time is required")
        LocalTime endTime,

        @NotNull(message = "Status is required")
        ClassroomStatus status,

        @Size(max = 1000, message = "Note must not exceed 1000 characters")
        String note
) {
}
