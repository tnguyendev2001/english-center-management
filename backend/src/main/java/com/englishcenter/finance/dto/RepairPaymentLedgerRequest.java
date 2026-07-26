package com.englishcenter.finance.dto;

import jakarta.validation.constraints.NotNull;

public record RepairPaymentLedgerRequest(
        @NotNull(message = "Payment id is required")
        Long paymentId
) {
}
