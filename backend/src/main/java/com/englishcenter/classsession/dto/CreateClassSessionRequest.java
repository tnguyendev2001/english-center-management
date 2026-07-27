package com.englishcenter.classsession.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

public record CreateClassSessionRequest(
        @NotNull(message = "Classroom id is required")
        Long classroomId,

        @NotNull(message = "Session date is required")
        LocalDate sessionDate,

        LocalTime startTime,

        LocalTime endTime,

        @Size(max = 1000, message = "Note must not exceed 1000 characters")
        String note
) {
}
