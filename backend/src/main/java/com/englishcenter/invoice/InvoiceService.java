package com.englishcenter.invoice;

import com.englishcenter.common.exception.NotFoundException;
import com.englishcenter.invoice.dto.InvoiceResponse;
import com.englishcenter.invoice.mapper.InvoiceMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceService {
    private static final int MAX_PAGE_SIZE = 100;

    private final InvoiceRepository invoiceRepository;
    private final InvoiceMapper invoiceMapper;

    public InvoiceService(InvoiceRepository invoiceRepository, InvoiceMapper invoiceMapper) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceMapper = invoiceMapper;
    }

    @Transactional(readOnly = true)
    public Page<InvoiceResponse> getInvoices(int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                normalizePageSize(size),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return invoiceRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(invoiceMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getById(Long id) {
        return invoiceMapper.toResponse(invoiceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Invoice not found")));
    }

    private int normalizePageSize(int size) {
        if (size <= 0) {
            return 20;
        }

        return Math.min(size, MAX_PAGE_SIZE);
    }
}
