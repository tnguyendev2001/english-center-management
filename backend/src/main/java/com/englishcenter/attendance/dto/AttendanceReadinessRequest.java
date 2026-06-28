package com.englishcenter.attendance.dto;

import jakarta.validation.constraints.NotNull;

public record AttendanceReadinessRequest(
        @NotNull Long sessionId
) {
}
