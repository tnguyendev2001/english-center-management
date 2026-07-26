package com.englishcenter.finance.dto;

public record PaymentReconciliationMismatchItem(
        String mismatchType,
        Long paymentId,
        String paymentCode,
        Long cashTransactionId,
        String transactionCode,
        String detail
) {
}
