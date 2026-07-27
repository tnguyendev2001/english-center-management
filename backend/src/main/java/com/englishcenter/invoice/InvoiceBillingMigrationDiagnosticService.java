package com.englishcenter.invoice;

import com.englishcenter.invoice.dto.AmbiguousInvoiceDiagnosticResponse;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceBillingMigrationDiagnosticService {
    private final InvoiceRepository invoiceRepository;

    public InvoiceBillingMigrationDiagnosticService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @Transactional(readOnly = true)
    public List<AmbiguousInvoiceDiagnosticResponse> findAmbiguousInvoices() {
        List<AmbiguousInvoiceDiagnosticResponse> result = new ArrayList<>();
        for (Invoice invoice : invoiceRepository.findAll()) {
            if (invoice.getCycleNo() != null) {
                continue;
            }
            Integer packageCycle = invoice.getStudentPackage() != null
                    ? invoice.getStudentPackage().getCycleNo()
                    : null;
            result.add(new AmbiguousInvoiceDiagnosticResponse(
                    invoice.getId(),
                    invoice.getInvoiceCode(),
                    invoice.getStudent() != null ? invoice.getStudent().getId() : null,
                    invoice.getStudent() != null ? invoice.getStudent().getStudentCode() : null,
                    invoice.getStudent() != null ? invoice.getStudent().getFullName() : null,
                    invoice.getEnrollment() != null ? invoice.getEnrollment().getId() : null,
                    invoice.getStudentPackage() != null ? invoice.getStudentPackage().getId() : null,
                    packageCycle,
                    invoice.getCycleNo(),
                    invoice.getCreatedAt() != null ? invoice.getCreatedAt().toLocalDate() : null,
                    packageCycle,
                    packageCycle == null
                            ? "Missing StudentPackage cycleNo and Invoice.cycleNo"
                            : "Invoice.cycleNo still null after linked StudentPackage backfill"
            ));
        }
        return result;
    }
}
