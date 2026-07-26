package com.englishcenter.finance.dto;

import com.englishcenter.finance.ReconciliationStatus;
import java.math.BigDecimal;
import java.util.List;

public record PaymentReconciliationResponse(
        BigDecimal totalValidPayments,
        BigDecimal totalPaymentAmount,
        BigDecimal totalTuitionLedgerAmount,
        BigDecimal difference,
        long matchedCount,
        long missingLedgerCount,
        long orphanLedgerCount,
        long duplicateCount,
        long amountMismatchCount,
        long cancellationMismatchCount,
        long accountMismatchCount,
        long dateMismatchCount,
        ReconciliationStatus status,
        List<PaymentReconciliationMismatchItem> mismatches
) {
}
