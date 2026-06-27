package com.englishcenter.invoice;

import com.englishcenter.common.api.ApiResponse;
import com.englishcenter.common.api.PageMeta;
import com.englishcenter.invoice.dto.InvoiceResponse;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {
    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping
    public ApiResponse<List<InvoiceResponse>> getInvoices(
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long classroomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<InvoiceResponse> invoices = invoiceService.getInvoices(status, studentId, classroomId, page, size);
        PageMeta meta = new PageMeta(
                invoices.getNumber(),
                invoices.getSize(),
                invoices.getTotalElements(),
                invoices.getTotalPages()
        );

        return ApiResponse.success(invoices.getContent(), meta);
    }

    @GetMapping("/{id}")
    public ApiResponse<InvoiceResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(invoiceService.getById(id));
    }
}
