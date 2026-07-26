package com.englishcenter.classsession.dto;

import java.time.LocalDate;
import java.util.List;

public record SessionGenerationPlan(
        Long classroomId,
        int existingSessionCount,
        int sessionsToCreate,
        int sessionsToReuse,
        LocalDate firstSessionDate,
        LocalDate lastGeneratedSessionDate,
        List<LocalDate> plannedDates
) {
}
