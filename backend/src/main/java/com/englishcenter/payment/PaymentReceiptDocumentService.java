package com.englishcenter.payment;

import com.englishcenter.center.CenterProfileService;
import com.englishcenter.center.dto.CenterProfileResponse;
import com.englishcenter.common.exception.NotFoundException;
import com.englishcenter.invoice.BillingCycleLabelService;
import com.englishcenter.invoice.Invoice;
import com.englishcenter.payment.dto.PaymentReceiptDocumentResponse;
import com.englishcenter.student.Student;
import com.englishcenter.studentpackage.StudentPackage;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentReceiptDocumentService {
    private final PaymentRepository paymentRepository;
    private final CenterProfileService centerProfileService;
    private final BillingCycleLabelService billingCycleLabelService;

    public PaymentReceiptDocumentService(
            PaymentRepository paymentRepository,
            CenterProfileService centerProfileService,
            BillingCycleLabelService billingCycleLabelService
    ) {
        this.paymentRepository = paymentRepository;
        this.centerProfileService = centerProfileService;
        this.billingCycleLabelService = billingCycleLabelService;
    }

    @Transactional(readOnly = true)
    public PaymentReceiptDocumentResponse getReceipt(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Payment not found"));
        Invoice invoice = payment.getInvoice();
        Student student = payment.getStudent();
        StudentPackage studentPackage = invoice.getStudentPackage();
        CenterProfileResponse center = centerProfileService.getProfile();

        Integer cycleNo = invoice.getCycleNo() != null
                ? invoice.getCycleNo()
                : (studentPackage != null ? studentPackage.getCycleNo() : null);
        LocalDate effectiveFrom = invoice.getEffectiveFrom() != null
                ? invoice.getEffectiveFrom()
                : (studentPackage != null ? studentPackage.getStartDate() : null);

        BigDecimal remainingAfter = null;
        if (payment.getStatus() == PaymentStatus.VALID) {
            remainingAfter = invoice.getRemainingAmount();
        }

        return new PaymentReceiptDocumentResponse(
                center,
                "PHIẾU THU",
                payment.getId(),
                payment.getPaymentCode(),
                payment.getPaymentDate(),
                payment.getStatus(),
                payment.getStatus() == PaymentStatus.CANCELED,
                student.getParentName() != null ? student.getParentName() : student.getFullName(),
                student.getStudentCode(),
                student.getFullName(),
                payment.getClassroom().getClassCode(),
                payment.getClassroom().getClassName(),
                invoice.getInvoiceCode(),
                billingCycleLabelService.buildBillingLabel(invoice.getPackageNameSnapshot(), cycleNo),
                invoice.getPackageNameSnapshot(),
                effectiveFrom,
                invoice.getFinalAmount(),
                remainingAfter,
                payment.getAmount(),
                null,
                payment.getMethod(),
                payment.getFinancialAccount() != null ? payment.getFinancialAccount().getName() : null,
                null,
                payment.getNote(),
                payment.getCreatedBy(),
                payment.getCreatedAt(),
                payment.getCreatedBy(),
                payment.getCanceledAt(),
                payment.getCanceledBy(),
                payment.getCancelReason(),
                center.receiptFooterNote()
        );
    }
}
