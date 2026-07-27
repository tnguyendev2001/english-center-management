package com.englishcenter.invoice;

import com.englishcenter.center.CenterProfileService;
import com.englishcenter.center.dto.CenterProfileResponse;
import com.englishcenter.common.exception.NotFoundException;
import com.englishcenter.enrollment.Enrollment;
import com.englishcenter.invoice.dto.InvoiceDocumentResponse;
import com.englishcenter.invoice.dto.InvoiceResponse;
import com.englishcenter.invoice.mapper.InvoiceMapper;
import com.englishcenter.payment.PaymentRepository;
import com.englishcenter.payment.dto.PaymentResponse;
import com.englishcenter.payment.mapper.PaymentMapper;
import com.englishcenter.student.Student;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceDocumentService {
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final InvoiceMapper invoiceMapper;
    private final PaymentMapper paymentMapper;
    private final CenterProfileService centerProfileService;
    private final BillingCycleLabelService billingCycleLabelService;

    public InvoiceDocumentService(
            InvoiceRepository invoiceRepository,
            PaymentRepository paymentRepository,
            InvoiceMapper invoiceMapper,
            PaymentMapper paymentMapper,
            CenterProfileService centerProfileService,
            BillingCycleLabelService billingCycleLabelService
    ) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.invoiceMapper = invoiceMapper;
        this.paymentMapper = paymentMapper;
        this.centerProfileService = centerProfileService;
        this.billingCycleLabelService = billingCycleLabelService;
    }

    @Transactional(readOnly = true)
    public InvoiceDocumentResponse getDocument(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new NotFoundException("Invoice not found"));
        InvoiceResponse invoiceResponse = invoiceMapper.toResponse(invoice);
        CenterProfileResponse center = centerProfileService.getProfile();
        Student student = invoice.getStudent();
        Enrollment enrollment = invoice.getEnrollment();

        List<PaymentResponse> payments = paymentRepository.findByInvoiceIdOrderByPaymentDateDescCreatedAtDesc(invoiceId)
                .stream()
                .map(paymentMapper::toResponse)
                .toList();

        InvoiceDocumentResponse.NextCycleSummary nextCycle = resolveNextCycle(invoice);
        Integer remainingSessions = enrollment != null
                ? Math.max(enrollment.getTotalSessions() - enrollment.getUsedSessions(), 0)
                : null;

        String estimatedDisplay = invoiceResponse.estimatedEffectiveTo() != null
                ? null
                : "Đến khi sử dụng hết "
                        + (invoiceResponse.totalSessionsSnapshot() != null
                        ? invoiceResponse.totalSessionsSnapshot()
                        : "")
                        + " buổi";

        String transferSuggestion = buildTransferSuggestion(student.getStudentCode(), invoice.getInvoiceCode());

        return new InvoiceDocumentResponse(
                center,
                "THÔNG BÁO HỌC PHÍ",
                invoice.getId(),
                invoice.getInvoiceCode(),
                invoiceResponse.issueDate(),
                invoice.getDueDate(),
                invoice.getStatus(),
                student.getStudentCode(),
                student.getFullName(),
                student.getParentName(),
                student.getParentPhone(),
                invoiceResponse.classroomCode(),
                invoiceResponse.classroomName(),
                invoiceResponse.teacherName(),
                invoiceResponse.packageNameSnapshot(),
                invoiceResponse.totalSessionsSnapshot(),
                invoiceResponse.packagePriceSnapshot(),
                invoiceResponse.cycleNo(),
                invoiceResponse.billingLabel(),
                invoiceResponse.effectiveFrom(),
                invoiceResponse.estimatedEffectiveTo(),
                estimatedDisplay,
                invoice.getFinalAmount(),
                invoice.getPaidAmount(),
                invoice.getRemainingAmount(),
                transferSuggestion,
                invoice.getNote(),
                nextCycle == null ? "Chưa tạo kỳ học phí tiếp theo" : null,
                nextCycle,
                remainingSessions,
                payments
        );
    }

    private InvoiceDocumentResponse.NextCycleSummary resolveNextCycle(Invoice invoice) {
        Integer cycleNo = invoice.getCycleNo();
        if (cycleNo == null && invoice.getStudentPackage() != null) {
            cycleNo = invoice.getStudentPackage().getCycleNo();
        }
        if (cycleNo == null || invoice.getEnrollment() == null) {
            return null;
        }

        int currentCycle = cycleNo;
        return invoiceRepository.findAllByEnrollmentIdOrderByCreatedAtDesc(invoice.getEnrollment().getId())
                .stream()
                .filter(other -> other.getCycleNo() != null && other.getCycleNo() > currentCycle)
                .filter(other -> other.getStatus() != InvoiceStatus.CANCELED)
                .min(Comparator.comparing(Invoice::getCycleNo).thenComparing(Invoice::getId))
                .map(next -> new InvoiceDocumentResponse.NextCycleSummary(
                        next.getId(),
                        next.getInvoiceCode(),
                        billingCycleLabelService.buildBillingLabel(next.getPackageNameSnapshot(), next.getCycleNo()),
                        next.getEffectiveFrom() != null
                                ? next.getEffectiveFrom()
                                : (next.getStudentPackage() != null ? next.getStudentPackage().getStartDate() : null),
                        next.getDueDate(),
                        next.getRemainingAmount()
                ))
                .orElse(null);
    }

    private String buildTransferSuggestion(String studentCode, String invoiceCode) {
        String code = studentCode != null ? studentCode : "";
        String invoice = invoiceCode != null ? invoiceCode : "";
        return (code + " " + invoice).trim();
    }
}
