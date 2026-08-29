package com.englishcenter.studentpackage.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record StudentPackagePeriodResponse(
        Long studentPackageId,
        Long enrollmentId,
        Integer cycleNo,
        String packageName,
        Integer packageSessionCount,
        LocalDate calculatedPeriodStartDate,
        LocalDate calculatedPeriodEndDate,
        LocalDate manualPeriodStartDate,
        LocalDate effectivePeriodStartDate,
        boolean periodNeedsRecalculation,
        String manualOverrideReason,
        LocalDateTime manualOverrideChangedAt,
        String manualOverrideChangedBy
) {
}
