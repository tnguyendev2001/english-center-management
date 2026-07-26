package com.englishcenter.finance.dto;

public record RepairPaymentLedgerResponse(
        Long paymentId,
        String paymentCode,
        boolean created,
        CashTransactionResponse transaction,
        String message
) {
}
