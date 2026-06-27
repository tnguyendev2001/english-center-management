package com.englishcenter.debt;

import com.englishcenter.common.api.ApiResponse;
import com.englishcenter.common.api.PageMeta;
import com.englishcenter.invoice.dto.InvoiceResponse;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DebtController {
    private final DebtService debtService;

    public DebtController(DebtService debtService) {
        this.debtService = debtService;
    }

    @GetMapping("/api/debts")
    public ApiResponse<List<InvoiceResponse>> getDebts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<InvoiceResponse> debts = debtService.getDebts(page, size);
        PageMeta meta = new PageMeta(
                debts.getNumber(),
                debts.getSize(),
                debts.getTotalElements(),
                debts.getTotalPages()
        );

        return ApiResponse.success(debts.getContent(), meta);
    }
}
