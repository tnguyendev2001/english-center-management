package com.englishcenter.attendance.dto;

import com.englishcenter.attendance.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AttendanceItemRequest(
        @NotNull(message = "Student id is required")
        Long studentId,

        @NotNull(message = "Attendance status is required")
        AttendanceStatus status,

        @Size(max = 1000, message = "Note must not exceed 1000 characters")
        String note
) {
}
