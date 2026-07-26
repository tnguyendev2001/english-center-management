package com.englishcenter.importdata.dto;

import java.util.List;

public record LegacyImportPreviewResponse(
        Long tuitionPackageId,
        String tuitionPackageName,
        int totalSheets,
        int totalRows,
        int validRows,
        int warningRows,
        int invalidRows,
        int newClassrooms,
        int existingClassrooms,
        int newStudents,
        int existingStudents,
        int newEnrollments,
        int duplicateEnrollments,
        boolean canConfirm,
        String confirmationWarning,
        List<LegacyImportSheetPreview> sheets,
        List<LegacyImportRowPreview> rows
) {
}
