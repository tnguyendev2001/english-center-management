package com.englishcenter.payment.mapper;

import com.englishcenter.payment.Payment;
import com.englishcenter.payment.dto.PaymentResponse;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {
    public PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getPaymentCode(),
                payment.getInvoice().getId(),
                payment.getInvoice().getInvoiceCode(),
                payment.getStudent().getId(),
                payment.getStudent().getStudentCode(),
                payment.getStudent().getFullName(),
                payment.getClassroom().getId(),
                payment.getClassroom().getClassName(),
                payment.getAmount(),
                payment.getPaymentDate(),
                payment.getMethod(),
                payment.getStatus(),
                payment.getNote(),
                payment.getCancelReason(),
                payment.getCreatedAt(),
                payment.getCanceledAt()
        );
    }
}
