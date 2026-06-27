package com.englishcenter.payment.dto;

import com.englishcenter.payment.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreatePaymentRequest(
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        BigDecimal amount,

        @NotNull(message = "Payment date is required")
        LocalDate paymentDate,

        @NotNull(message = "Payment method is required")
        PaymentMethod method,

        @Size(max = 1000, message = "Note must not exceed 1000 characters")
        String note
) {
}
