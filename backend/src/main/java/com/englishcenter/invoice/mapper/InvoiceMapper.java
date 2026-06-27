package com.englishcenter.invoice.mapper;

import com.englishcenter.invoice.Invoice;
import com.englishcenter.invoice.dto.InvoiceResponse;
import org.springframework.stereotype.Component;

@Component
public class InvoiceMapper {
    public InvoiceResponse toResponse(Invoice invoice) {
        return new InvoiceResponse(
                invoice.getId(),
                invoice.getInvoiceCode(),
                invoice.getStudent().getId(),
                invoice.getStudent().getFullName(),
                invoice.getClassroom().getId(),
                invoice.getClassroom().getClassName(),
                invoice.getEnrollment().getId(),
                invoice.getStudentPackage().getId(),
                invoice.getPackageNameSnapshot(),
                invoice.getTotalSessionsSnapshot(),
                invoice.getAmount(),
                invoice.getDiscountAmount(),
                invoice.getAdjustmentAmount(),
                invoice.getFinalAmount(),
                invoice.getPaidAmount(),
                invoice.getRemainingAmount(),
                invoice.getDueDate(),
                invoice.getStatus(),
                invoice.getNote(),
                invoice.getCreatedAt(),
                invoice.getUpdatedAt()
        );
    }
}
