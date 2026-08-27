package com.englishcenter.debt;

import com.englishcenter.classroom.Classroom;
import com.englishcenter.debt.dto.StudentDebtSummaryResponse;
import com.englishcenter.financial.StudentCurrentClassroomResolver;
import com.englishcenter.financial.StudentFinancialSummaryAggregator;
import com.englishcenter.financial.StudentSummaryQuerySupport;
import com.englishcenter.invoice.Invoice;
import com.englishcenter.invoice.InvoiceRepository;
import com.englishcenter.invoice.dto.InvoiceResponse;
import com.englishcenter.invoice.mapper.InvoiceMapper;
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
public class DebtService {
    private static final int MAX_PAGE_SIZE = 100;

    private final InvoiceRepository invoiceRepository;
    private final InvoiceMapper invoiceMapper;
    private final StudentCurrentClassroomResolver currentClassroomResolver;

    public DebtService(
            InvoiceRepository invoiceRepository,
            InvoiceMapper invoiceMapper,
            StudentCurrentClassroomResolver currentClassroomResolver
    ) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceMapper = invoiceMapper;
        this.currentClassroomResolver = currentClassroomResolver;
    }

    @Transactional(readOnly = true)
    public Page<StudentDebtSummaryResponse> getStudentSummaries(String keyword, int page, int size) {
        List<Invoice> invoices = invoiceRepository.findAllForDebtSummary(null);
        List<StudentDebtSummaryResponse> summaries = enrichCurrentClassrooms(
                StudentFinancialSummaryAggregator.aggregateStudentDebtSummaries(invoices)
        );
        Map<Long, String> phonesByStudentId = phonesByStudentId(invoices);
        List<StudentDebtSummaryResponse> filtered = summaries.stream()
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
    public Page<InvoiceResponse> getDebts(int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                normalizePageSize(size),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return invoiceRepository.findDebtInvoices(pageable)
                .map(invoiceMapper::toResponse);
    }

    private List<StudentDebtSummaryResponse> enrichCurrentClassrooms(List<StudentDebtSummaryResponse> summaries) {
        Map<Long, Classroom> classrooms = currentClassroomResolver.resolve(
                summaries.stream().map(StudentDebtSummaryResponse::studentId).toList()
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

    private int normalizePageSize(int size) {
        if (size <= 0) {
            return 20;
        }

        return Math.min(size, MAX_PAGE_SIZE);
    }
}
