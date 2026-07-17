package com.englishcenter.debt;

import com.englishcenter.debt.dto.StudentDebtSummaryResponse;
import com.englishcenter.financial.StudentFinancialSummaryAggregator;
import com.englishcenter.invoice.InvoiceRepository;
import com.englishcenter.invoice.dto.InvoiceResponse;
import com.englishcenter.invoice.mapper.InvoiceMapper;
import java.util.List;
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

    public DebtService(InvoiceRepository invoiceRepository, InvoiceMapper invoiceMapper) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceMapper = invoiceMapper;
    }

    @Transactional(readOnly = true)
    public List<StudentDebtSummaryResponse> getStudentSummaries(Long classroomId) {
        return StudentFinancialSummaryAggregator.aggregateDebtSummaries(
                invoiceRepository.findAllForDebtSummary(classroomId)
        );
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

    private int normalizePageSize(int size) {
        if (size <= 0) {
            return 20;
        }

        return Math.min(size, MAX_PAGE_SIZE);
    }
}
