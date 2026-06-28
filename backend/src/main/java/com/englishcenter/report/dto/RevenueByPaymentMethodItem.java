package com.englishcenter.report.dto;

import com.englishcenter.payment.PaymentMethod;
import java.math.BigDecimal;

public record RevenueByPaymentMethodItem(
        PaymentMethod paymentMethod,
        BigDecimal amount
) {
}
