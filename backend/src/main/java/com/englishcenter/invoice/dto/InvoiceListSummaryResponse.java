package com.englishcenter.invoice.dto;

import java.math.BigDecimal;

public record InvoiceListSummaryResponse(
        BigDecimal totalRemainingCollectible,
        long unpaidCount,
        long partiallyPaidCount,
        BigDecimal collectedThisMonth
) {
}
