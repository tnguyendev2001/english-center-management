package com.englishcenter.debt;

import com.englishcenter.common.config.AppTimeProperties;
import com.englishcenter.debt.dto.StudentDebtSummaryResponse;
import com.englishcenter.financial.StudentFinancialSummaryAggregator;
import com.englishcenter.invoice.InvoiceRepository;
import com.englishcenter.invoice.InvoiceStatus;
import com.englishcenter.invoice.dto.InvoiceResponse;
import com.englishcenter.invoice.mapper.InvoiceMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DebtService {
    private static final int MAX_PAGE_SIZE = 100;

    private final InvoiceRepository invoiceRepository;
    private final InvoiceMapper invoiceMapper;
    private final AppTimeProperties appTimeProperties;

    public DebtService(
            InvoiceRepository invoiceRepository,
            InvoiceMapper invoiceMapper,
            AppTimeProperties appTimeProperties
    ) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceMapper = invoiceMapper;
        this.appTimeProperties = appTimeProperties;
    }

    @Transactional(readOnly = true)
    public List<StudentDebtSummaryResponse> getStudentSummaries(Long classroomId) {
        return StudentFinancialSummaryAggregator.aggregateDebtSummaries(
                invoiceRepository.findAllForDebtSummary(classroomId)
        );
    }

    @Transactional(readOnly = true)
    public Page<InvoiceResponse> getDebts(
            Long classroomId,
            Long packageId,
            Integer cycleNo,
            Boolean overdue,
            Boolean multipleInvoices,
            LocalDate dueFrom,
            LocalDate dueTo,
            BigDecimal remainingFrom,
            BigDecimal remainingTo,
            String keyword,
            String debtStatus,
            int page,
            int size
    ) {
        LocalDate today = LocalDate.now(appTimeProperties.zoneId());
        List<InvoiceResponse> debts = invoiceRepository.findAllForDebtSummary(classroomId).stream()
                .map(invoiceMapper::toResponse)
                .filter(invoice -> packageId == null || packageId.equals(invoice.packageId()))
                .filter(invoice -> cycleNo == null || cycleNo.equals(invoice.cycleNo()))
                .filter(invoice -> overdue == null || overdue.equals(invoice.overdueDays() != null && invoice.overdueDays() > 0))
                .filter(invoice -> dueFrom == null || (invoice.dueDate() != null && !invoice.dueDate().isBefore(dueFrom)))
                .filter(invoice -> dueTo == null || (invoice.dueDate() != null && !invoice.dueDate().isAfter(dueTo)))
                .filter(invoice -> remainingFrom == null
                        || (invoice.remainingAmount() != null && invoice.remainingAmount().compareTo(remainingFrom) >= 0))
                .filter(invoice -> remainingTo == null
                        || (invoice.remainingAmount() != null && invoice.remainingAmount().compareTo(remainingTo) <= 0))
                .filter(invoice -> matchesKeyword(invoice, keyword))
                .filter(invoice -> matchesDebtStatus(invoice, debtStatus, today))
                .sorted(debtComparator())
                .toList();

        if (Boolean.TRUE.equals(multipleInvoices)) {
            var counts = debts.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            InvoiceResponse::studentId,
                            java.util.stream.Collectors.counting()
                    ));
            debts = debts.stream()
                    .filter(invoice -> counts.getOrDefault(invoice.studentId(), 0L) >= 2)
                    .toList();
        }

        int pageIndex = Math.max(page, 0);
        int pageSize = normalizePageSize(size);
        int from = Math.min(pageIndex * pageSize, debts.size());
        int to = Math.min(from + pageSize, debts.size());
        Pageable pageable = PageRequest.of(pageIndex, pageSize);
        return new PageImpl<>(debts.subList(from, to), pageable, debts.size());
    }

    private boolean matchesKeyword(InvoiceResponse invoice, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String q = keyword.trim().toLowerCase();
        return (invoice.studentCode() != null && invoice.studentCode().toLowerCase().contains(q))
                || (invoice.studentName() != null && invoice.studentName().toLowerCase().contains(q))
                || (invoice.invoiceCode() != null && invoice.invoiceCode().toLowerCase().contains(q));
    }

    private boolean matchesDebtStatus(InvoiceResponse invoice, String debtStatus, LocalDate today) {
        if (debtStatus == null || debtStatus.isBlank()) {
            return true;
        }
        String normalized = debtStatus.trim().toUpperCase();
        if ("OVERDUE".equals(normalized)) {
            return invoice.overdueDays() != null && invoice.overdueDays() > 0;
        }
        if ("DUE_TODAY".equals(normalized)) {
            return invoice.dueDate() != null && invoice.dueDate().isEqual(today);
        }
        if ("NOT_DUE".equals(normalized)) {
            return "NOT_DUE".equals(invoice.debtStatus());
        }
        if ("PARTIALLY_PAID".equals(normalized)) {
            return invoice.status() == InvoiceStatus.PARTIALLY_PAID;
        }
        if ("OUTSTANDING".equals(normalized)) {
            return true;
        }
        return true;
    }

    private Comparator<InvoiceResponse> debtComparator() {
        return Comparator
                .comparing((InvoiceResponse invoice) -> invoice.overdueDays() == null ? 0 : invoice.overdueDays(),
                        Comparator.reverseOrder())
                .thenComparing(InvoiceResponse::dueDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(InvoiceResponse::createdAt, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private int normalizePageSize(int size) {
        if (size <= 0) {
            return 20;
        }

        return Math.min(size, MAX_PAGE_SIZE);
    }
}
