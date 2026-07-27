package com.englishcenter.payment.mapper;

import com.englishcenter.invoice.BillingCycleLabelService;
import com.englishcenter.invoice.Invoice;
import com.englishcenter.payment.Payment;
import com.englishcenter.payment.dto.PaymentResponse;
import com.englishcenter.studentpackage.StudentPackage;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {
    private final BillingCycleLabelService billingCycleLabelService;

    public PaymentMapper(BillingCycleLabelService billingCycleLabelService) {
        this.billingCycleLabelService = billingCycleLabelService;
    }

    public PaymentResponse toResponse(Payment payment) {
        Invoice invoice = payment.getInvoice();
        StudentPackage studentPackage = invoice != null ? invoice.getStudentPackage() : null;
        Integer cycleNo = invoice != null && invoice.getCycleNo() != null
                ? invoice.getCycleNo()
                : (studentPackage != null ? studentPackage.getCycleNo() : null);
        String packageName = invoice != null ? invoice.getPackageNameSnapshot() : null;
        LocalDate effectiveFrom = invoice != null
                ? (invoice.getEffectiveFrom() != null
                ? invoice.getEffectiveFrom()
                : (studentPackage != null ? studentPackage.getStartDate() : null))
                : null;

        return new PaymentResponse(
                payment.getId(),
                payment.getPaymentCode(),
                invoice != null ? invoice.getId() : null,
                invoice != null ? invoice.getInvoiceCode() : null,
                cycleNo,
                billingCycleLabelService.buildBillingLabel(packageName, cycleNo),
                packageName,
                effectiveFrom,
                payment.getStudent().getId(),
                payment.getStudent().getStudentCode(),
                payment.getStudent().getFullName(),
                payment.getClassroom().getId(),
                payment.getClassroom().getClassCode(),
                payment.getClassroom().getClassName(),
                payment.getAmount(),
                payment.getPaymentDate(),
                payment.getMethod(),
                payment.getFinancialAccount() != null ? payment.getFinancialAccount().getId() : null,
                payment.getFinancialAccount() != null ? payment.getFinancialAccount().getCode() : null,
                payment.getFinancialAccount() != null ? payment.getFinancialAccount().getName() : null,
                payment.getStatus(),
                payment.getNote(),
                payment.getCancelReason(),
                payment.getCreatedBy(),
                payment.getCanceledBy(),
                payment.getCreatedAt(),
                payment.getCanceledAt()
        );
    }
}
