package com.englishcenter.report.dto;

import com.englishcenter.payment.dto.PaymentResponse;
import java.math.BigDecimal;
import java.util.List;

public record PaymentReportResponse(
        BigDecimal totalRevenue,
        long paymentCount,
        BigDecimal cashAmount,
        BigDecimal bankTransferAmount,
        List<RevenueByDateItem> revenueByDate,
        List<PaymentResponse> payments
) {
}
