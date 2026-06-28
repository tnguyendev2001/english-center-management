package com.englishcenter.classroom.dto;

import com.englishcenter.studentpackage.StudentPackageStatus;
import java.math.BigDecimal;

public record ClassroomRenewalConfirmItemResponse(
        Long studentId,
        Long enrollmentId,
        String studentName,
        Long newStudentPackageId,
        Long newInvoiceId,
        String newPackageName,
        BigDecimal newInvoiceAmount,
        StudentPackageStatus newStudentPackageStatus
) {
}
