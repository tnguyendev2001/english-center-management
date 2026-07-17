package com.englishcenter.payment;

import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.common.exception.NotFoundException;
import com.englishcenter.invoice.Invoice;
import com.englishcenter.invoice.InvoiceRepository;
import com.englishcenter.invoice.InvoiceService;
import com.englishcenter.invoice.InvoiceStatus;
import com.englishcenter.financial.StudentFinancialSummaryAggregator;
import com.englishcenter.payment.dto.CancelPaymentRequest;
import com.englishcenter.payment.dto.CreatePaymentRequest;
import com.englishcenter.payment.dto.PaymentResponse;
import com.englishcenter.payment.dto.StudentPaymentSummaryResponse;
import com.englishcenter.payment.mapper.PaymentMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceService invoiceService;
    private final PaymentMapper paymentMapper;

    public PaymentService(
            PaymentRepository paymentRepository,
            InvoiceRepository invoiceRepository,
            InvoiceService invoiceService,
            PaymentMapper paymentMapper
    ) {
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
        this.invoiceService = invoiceService;
        this.paymentMapper = paymentMapper;
    }

    @Transactional(readOnly = true)
    public List<StudentPaymentSummaryResponse> getStudentSummaries(LocalDate fromDate, LocalDate toDate) {
        return StudentFinancialSummaryAggregator.aggregatePaymentSummaries(
                paymentRepository.findAllForPaymentSummary(),
                fromDate,
                toDate
        );
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPayments(int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                normalizePageSize(size),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return paymentRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(paymentMapper::toResponse);
    }

    @Transactional
    public PaymentResponse createPayment(Long invoiceId, CreatePaymentRequest request) {
        Invoice invoice = findInvoice(invoiceId);
        invoice = invoiceService.recalculateAndSave(invoice);

        validateInvoiceCanReceivePayment(invoice);
        validatePaymentAmount(request.amount(), invoice.getRemainingAmount());

        Payment payment = new Payment();
        payment.setPaymentCode(generatePaymentCode());
        payment.setInvoice(invoice);
        payment.setStudent(invoice.getStudent());
        payment.setClassroom(invoice.getClassroom());
        payment.setAmount(request.amount());
        payment.setPaymentDate(request.paymentDate());
        payment.setMethod(request.method());
        payment.setStatus(PaymentStatus.VALID);
        payment.setNote(trimToNull(request.note()));

        payment = paymentRepository.save(payment);
        invoiceService.recalculateAndSave(invoice);

        // TODO: Save ActivityLog for CREATE_PAYMENT when the ActivityLog module exists.
        return paymentMapper.toResponse(payment);
    }

    @Transactional
    public PaymentResponse cancelPayment(Long paymentId, CancelPaymentRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Payment not found"));

        if (payment.getStatus() == PaymentStatus.CANCELED) {
            throw new BusinessException("Payment is already canceled");
        }

        payment.setStatus(PaymentStatus.CANCELED);
        payment.setCancelReason(request.reason().trim());
        payment.setCanceledAt(LocalDateTime.now());

        payment = paymentRepository.save(payment);
        invoiceService.recalculateAndSave(payment.getInvoice());

        // TODO: Save ActivityLog for CANCEL_PAYMENT when the ActivityLog module exists.
        return paymentMapper.toResponse(payment);
    }

    private Invoice findInvoice(Long invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new NotFoundException("Invoice not found"));
    }

    private void validateInvoiceCanReceivePayment(Invoice invoice) {
        if (invoice.getStatus() == InvoiceStatus.CANCELED) {
            throw new BusinessException("Cannot create payment for canceled invoice");
        }

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BusinessException("Cannot create payment for paid invoice");
        }
    }

    private void validatePaymentAmount(BigDecimal amount, BigDecimal remainingAmount) {
        if (amount == null || amount.compareTo(ZERO) <= 0) {
            throw new BusinessException("Payment amount must be greater than 0");
        }

        if (amount.compareTo(remainingAmount) > 0) {
            throw new BusinessException("Payment amount must not exceed invoice remaining amount");
        }
    }

    private String generatePaymentCode() {
        return "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private int normalizePageSize(int size) {
        if (size <= 0) {
            return 20;
        }

        return Math.min(size, MAX_PAGE_SIZE);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
