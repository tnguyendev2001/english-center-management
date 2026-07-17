package com.englishcenter.payment.dto;

import com.englishcenter.payment.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDate;

public record StudentPaymentSummaryResponse(
        Long studentId,
        String studentCode,
        String studentName,
        Long classroomId,
        String classroomName,
        BigDecimal totalPaidAmount,
        int paymentCount,
        LocalDate lastPaymentDate,
        PaymentMethod lastPaymentMethod
) {
}
