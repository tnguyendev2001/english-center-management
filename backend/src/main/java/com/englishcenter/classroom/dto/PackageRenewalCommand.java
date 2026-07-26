package com.englishcenter.classroom.dto;

import com.englishcenter.studentpackage.StudentPackageSourceType;
import java.time.LocalDate;

public record PackageRenewalCommand(
        Long enrollmentId,
        Long tuitionPackageId,
        LocalDate effectiveDate,
        StudentPackageSourceType sourceType,
        String invoiceNote,
        boolean createUnpaidInvoice
) {
}
