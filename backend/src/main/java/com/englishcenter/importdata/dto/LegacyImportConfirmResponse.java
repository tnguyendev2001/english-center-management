package com.englishcenter.importdata.dto;

import java.util.List;

public record LegacyImportConfirmResponse(
        int classroomsCreated,
        int classroomsReused,
        int studentsCreated,
        int studentsReused,
        int enrollmentsCreated,
        int enrollmentsSkipped,
        int sessionsCreated,
        int sessionsReused,
        int attendancesCreated,
        int invoicesCreated,
        int packageCyclesCreated,
        List<String> warnings,
        List<String> errors
) {
}
