package com.englishcenter.invoice;

import com.englishcenter.common.config.CenterProperties;
import com.englishcenter.common.exception.NotFoundException;
import com.englishcenter.invoice.TuitionPeriodResolver.TuitionPeriod;
import com.englishcenter.invoice.dto.TuitionNoticeResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TuitionNoticeService {
    private final InvoiceRepository invoiceRepository;
    private final TuitionPeriodResolver tuitionPeriodResolver;
    private final CenterProperties centerProperties;

    public TuitionNoticeService(
            InvoiceRepository invoiceRepository,
            TuitionPeriodResolver tuitionPeriodResolver,
            CenterProperties centerProperties
    ) {
        this.invoiceRepository = invoiceRepository;
        this.tuitionPeriodResolver = tuitionPeriodResolver;
        this.centerProperties = centerProperties;
    }

    @Transactional(readOnly = true)
    public TuitionNoticeResponse getByInvoiceId(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new NotFoundException("Invoice not found"));
        TuitionPeriod period = tuitionPeriodResolver.resolve(invoice);

        return new TuitionNoticeResponse(
                invoice.getId(),
                invoice.getInvoiceCode(),
                centerProperties.getCenterName(),
                blankToNull(centerProperties.getCenterAddress()),
                blankToNull(centerProperties.getCenterPhone()),
                invoice.getCreatedAt().toLocalDate(),
                invoice.getStudent().getStudentCode(),
                invoice.getStudent().getFullName(),
                invoice.getClassroom().getClassName(),
                invoice.getPackageNameSnapshot(),
                invoice.getTotalSessionsSnapshot(),
                period.start(),
                period.end(),
                invoice.getFinalAmount(),
                invoice.getPaidAmount(),
                invoice.getRemainingAmount(),
                VndAmountInWords.convert(invoice.getFinalAmount()),
                invoice.getDueDate(),
                invoice.getStatus(),
                centerProperties.getTuitionNoticeText(),
                centerProperties.getTuitionNoticeFooter()
        );
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
