package com.englishcenter.attendance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record MarkAttendanceRequest(
        @NotNull(message = "Session id is required")
        Long sessionId,

        @NotEmpty(message = "Attendance items are required")
        List<@Valid AttendanceItemRequest> items
) {
}
