package com.englishcenter.packagechange.dto;

import com.englishcenter.packagechange.PackageChangeAdjustmentType;
import com.englishcenter.packagechange.PackageChangeMode;
import java.math.BigDecimal;
import java.util.List;

public record ChangePackagePreviewResponse(
        Long oldStudentPackageId,
        PackageChangeMode changeMode,
        List<PackageChangeMode> allowedModes,
        String oldPackageName,
        Integer oldTotalSessions,
        BigDecimal oldPackagePrice,
        BigDecimal oldFinalAmount,
        Integer usedSessions,
        Integer remainingSessions,
        Integer remainingSessionsAfterChange,
        Integer makeupAvailableSessions,
        BigDecimal oldUnitPrice,
        BigDecimal usedAmount,
        BigDecimal totalValidPaidAmount,
        BigDecimal paidAmount,
        BigDecimal adjustmentAmount,
        PackageChangeAdjustmentType adjustmentType,
        BigDecimal unusedCredit,
        BigDecimal oldDebt,
        Long newTuitionPackageId,
        String newPackageName,
        Integer newTotalSessions,
        BigDecimal newPackagePrice,
        BigDecimal amountToPay,
        BigDecimal collectibleAmount,
        BigDecimal newInvoiceAdjustmentAmount,
        BigDecimal newInvoiceFinalAmount,
        String warningMessage
) {
}
