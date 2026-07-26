package com.englishcenter.classroom.dto;

public record PackageRenewalResult(
        Long enrollmentId,
        Long studentPackageId,
        Long invoiceId,
        int cycleNo,
        boolean created
) {
}
