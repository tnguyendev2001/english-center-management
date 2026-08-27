package com.englishcenter.importdata.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record LegacyImportRowPreview(
        int sheetIndex,
        String sheetName,
        int excelRowNumber,
        String studentName,
        String phone,
        LocalDate learningStartDate,
        LocalDate classroomStartDate,
        int eligibleSessionCount,
        int presentCount,
        int absentCount,
        int excusedCount,
        int consumingSessionCount,
        List<LocalDate> presentDates,
        List<LocalDate> absentDates,
        List<LocalDate> excusedDates,
        int packageCycles,
        int totalSessionsAfterImport,
        int usedSessionsAfterImport,
        int remainingSessionsAfterImport,
        int unpaidInvoicesToCreate,
        BigDecimal totalDebt,
        List<LegacyImportRowAction> actions,
        LegacyImportRowStatus status,
        Long existingStudentId,
        Long existingClassroomId,
        Long existingEnrollmentId,
        List<String> errors,
        List<String> warnings
) {
}
