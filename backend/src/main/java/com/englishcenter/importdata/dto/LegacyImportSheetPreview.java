package com.englishcenter.importdata.dto;

import com.englishcenter.classroom.ClassDayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record LegacyImportSheetPreview(
        int sheetIndex,
        String sheetName,
        String normalizedClassroomName,
        String proposedClassCode,
        LocalDate classroomStartDate,
        Set<ClassDayOfWeek> daysOfWeek,
        boolean classroomExists,
        Long existingClassroomId,
        int existingSessionCount,
        int sessionsToCreate,
        LocalDate firstSessionDate,
        LocalDate lastGeneratedSessionDate,
        List<String> errors,
        List<String> warnings,
        List<LegacyImportRowPreview> rows
) {
}
