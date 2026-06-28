package com.englishcenter.report.dto;

import com.englishcenter.payment.dto.PaymentResponse;
import java.math.BigDecimal;
import java.util.List;

public record RevenueReportResponse(
        BigDecimal totalRevenue,
        List<RevenueByDateItem> revenueByDate,
        List<RevenueByPaymentMethodItem> revenueByPaymentMethod,
        List<PaymentResponse> payments
) {
}
