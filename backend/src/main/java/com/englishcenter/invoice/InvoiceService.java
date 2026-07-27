package com.englishcenter.invoice;

import com.englishcenter.common.config.AppTimeProperties;
import com.englishcenter.common.exception.NotFoundException;
import com.englishcenter.financial.StudentFinancialSummaryAggregator;
import com.englishcenter.invoice.dto.InvoiceListSummaryResponse;
import com.englishcenter.invoice.dto.InvoiceResponse;
import com.englishcenter.invoice.dto.StudentTuitionSummaryResponse;
import com.englishcenter.invoice.mapper.InvoiceMapper;
import com.englishcenter.payment.PaymentRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final InvoiceMapper invoiceMapper;
    private final AppTimeProperties appTimeProperties;

    public InvoiceService(
            InvoiceRepository invoiceRepository,
            PaymentRepository paymentRepository,
            InvoiceMapper invoiceMapper,
            AppTimeProperties appTimeProperties
    ) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.invoiceMapper = invoiceMapper;
        this.appTimeProperties = appTimeProperties;
    }

    @Transactional(readOnly = true)
    public Page<InvoiceResponse> getInvoices(
            InvoiceStatus status,
            Long studentId,
            Long classroomId,
            Long packageId,
            Integer cycleNo,
            Boolean overdue,
            Boolean hasRemainingDebt,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            LocalDate dueFrom,
            LocalDate dueTo,
            String keyword,
            int page,
            int size
    ) {
        LocalDate today = LocalDate.now(appTimeProperties.zoneId());
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                normalizePageSize(size),
                Sort.by(
                        Sort.Order.asc("dueDate"),
                        Sort.Order.desc("createdAt")
                )
        );

        return invoiceRepository.search(
                        status,
                        studentId,
                        classroomId,
                        packageId,
                        cycleNo,
                        overdue,
                        hasRemainingDebt,
                        effectiveFrom,
                        effectiveTo,
                        dueFrom,
                        dueTo,
                        keyword,
                        today,
                        pageable
                )
                .map(invoiceMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public InvoiceListSummaryResponse getListSummary(Long classroomId) {
        LocalDate today = LocalDate.now(appTimeProperties.zoneId());
        YearMonth month = YearMonth.from(today);
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();

        BigDecimal totalRemaining = classroomId == null
                ? invoiceRepository.sumDebtAmount()
                : invoiceRepository.sumDebtAmountByClassroomId(classroomId);
        long unpaid = invoiceRepository.countByStatus(InvoiceStatus.UNPAID);
        long partial = invoiceRepository.countByStatus(InvoiceStatus.PARTIALLY_PAID);
        BigDecimal collectedThisMonth = classroomId == null
                ? paymentRepository.sumValidAmountBetween(monthStart, monthEnd)
                : paymentRepository.sumValidAmountByClassroomBetween(classroomId, monthStart, monthEnd);

        return new InvoiceListSummaryResponse(
                totalRemaining != null ? totalRemaining : ZERO,
                unpaid,
                partial,
                collectedThisMonth != null ? collectedThisMonth : ZERO
        );
    }

    @Transactional(readOnly = true)
    public List<StudentTuitionSummaryResponse> getStudentSummaries(Long classroomId) {
        return StudentFinancialSummaryAggregator.aggregateTuitionSummaries(
                invoiceRepository.findAllForTuitionSummary(classroomId)
        );
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getById(Long id) {
        return invoiceMapper.toResponse(invoiceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Invoice not found")));
    }

    @Transactional
    public Invoice recalculateAndSave(Invoice invoice) {
        if (invoice.getStatus() == InvoiceStatus.CANCELED) {
            return invoiceRepository.save(invoice);
        }

        BigDecimal paidAmount = paymentRepository.sumValidAmountByInvoiceId(invoice.getId());
        BigDecimal remainingAmount = invoice.getFinalAmount().subtract(paidAmount);

        invoice.setPaidAmount(paidAmount);
        invoice.setRemainingAmount(remainingAmount);
        invoice.setStatus(resolveStatus(invoice.getFinalAmount(), paidAmount));

        return invoiceRepository.save(invoice);
    }

    private InvoiceStatus resolveStatus(BigDecimal finalAmount, BigDecimal paidAmount) {
        if (paidAmount.compareTo(ZERO) <= 0) {
            return InvoiceStatus.UNPAID;
        }

        if (paidAmount.compareTo(finalAmount) < 0) {
            return InvoiceStatus.PARTIALLY_PAID;
        }

        return InvoiceStatus.PAID;
    }

    private int normalizePageSize(int size) {
        if (size <= 0) {
            return 20;
        }

        return Math.min(size, MAX_PAGE_SIZE);
    }
}
