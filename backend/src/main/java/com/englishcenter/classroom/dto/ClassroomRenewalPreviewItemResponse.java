package com.englishcenter.classroom.dto;

import com.englishcenter.studentpackage.StudentPackageStatus;
import java.math.BigDecimal;

public record ClassroomRenewalPreviewItemResponse(
        Long studentId,
        Long enrollmentId,
        Long currentStudentPackageId,
        String studentName,
        String currentPackageName,
        int usedSessions,
        int remainingSessions,
        Long newTuitionPackageId,
        String newPackageName,
        int newPackageTotalSessions,
        BigDecimal newInvoiceAmount,
        StudentPackageStatus newStudentPackageStatus,
        boolean eligible,
        String warning
) {
}
