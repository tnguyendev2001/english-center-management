package com.englishcenter.classsession.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record GenerateClassSessionsRequest(
        @NotNull(message = "Classroom id is required")
        Long classroomId,

        @Min(value = 1, message = "Number of sessions must be greater than 0")
        Integer numberOfSessions,

        LocalDate fromDate,

        LocalDate toDate
) {
}
