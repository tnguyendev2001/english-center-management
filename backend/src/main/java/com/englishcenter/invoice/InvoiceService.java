package com.englishcenter.invoice;

import com.englishcenter.classroom.Classroom;
import com.englishcenter.common.exception.NotFoundException;
import com.englishcenter.financial.StudentCurrentClassroomResolver;
import com.englishcenter.financial.StudentFinancialSummaryAggregator;
import com.englishcenter.financial.StudentSummaryQuerySupport;
import com.englishcenter.invoice.dto.InvoiceResponse;
import com.englishcenter.invoice.dto.StudentTuitionSummaryResponse;
import com.englishcenter.invoice.dto.TuitionOverviewResponse;
import com.englishcenter.invoice.mapper.InvoiceMapper;
import com.englishcenter.payment.PaymentRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final StudentCurrentClassroomResolver currentClassroomResolver;

    public InvoiceService(
            InvoiceRepository invoiceRepository,
            PaymentRepository paymentRepository,
            InvoiceMapper invoiceMapper,
            StudentCurrentClassroomResolver currentClassroomResolver
    ) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.invoiceMapper = invoiceMapper;
        this.currentClassroomResolver = currentClassroomResolver;
    }

    @Transactional(readOnly = true)
    public Page<InvoiceResponse> getInvoices(
            InvoiceStatus status,
            Long studentId,
            Long classroomId,
            String keyword,
            String packageName,
            LocalDate dueFrom,
            LocalDate dueTo,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                normalizePageSize(size),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return invoiceRepository.search(
                        status,
                        studentId,
                        classroomId,
                        blankToNull(keyword),
                        blankToNull(packageName),
                        dueFrom,
                        dueTo,
                        pageable
                )
                .map(invoiceMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<StudentTuitionSummaryResponse> getStudentSummaries(String keyword, int page, int size) {
        List<Invoice> invoices = invoiceRepository.findAllForTuitionSummary(null);
        List<StudentTuitionSummaryResponse> summaries = enrichCurrentClassrooms(
                StudentFinancialSummaryAggregator.aggregateStudentTuitionSummaries(invoices)
        );
        Map<Long, String> phonesByStudentId = phonesByStudentId(invoices);
        List<StudentTuitionSummaryResponse> filtered = summaries.stream()
                .filter(summary -> StudentSummaryQuerySupport.matchesKeyword(
                        keyword,
                        summary.studentCode(),
                        summary.studentName(),
                        phonesByStudentId.get(summary.studentId())
                ))
                .toList();
        return StudentSummaryQuerySupport.paginate(filtered, page, size, MAX_PAGE_SIZE);
    }

    @Transactional(readOnly = true)
    public TuitionOverviewResponse getOverview() {
        return new TuitionOverviewResponse(
                invoiceRepository.sumDebtAmount(),
                invoiceRepository.countDistinctStudentsWithDebt(),
                invoiceRepository.countByStatus(InvoiceStatus.UNPAID),
                invoiceRepository.countByStatus(InvoiceStatus.PARTIALLY_PAID)
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

    private List<StudentTuitionSummaryResponse> enrichCurrentClassrooms(List<StudentTuitionSummaryResponse> summaries) {
        Map<Long, Classroom> classrooms = currentClassroomResolver.resolve(
                summaries.stream().map(StudentTuitionSummaryResponse::studentId).toList()
        );
        return summaries.stream()
                .map(summary -> {
                    Classroom classroom = classrooms.get(summary.studentId());
                    if (classroom == null) {
                        return summary;
                    }
                    return summary.withCurrentClassroom(classroom.getId(), classroom.getClassName());
                })
                .toList();
    }

    private Map<Long, String> phonesByStudentId(List<Invoice> invoices) {
        Map<Long, String> phones = new HashMap<>();
        for (Invoice invoice : invoices) {
            phones.putIfAbsent(invoice.getStudent().getId(), invoice.getStudent().getPhone());
        }
        return phones;
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

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
