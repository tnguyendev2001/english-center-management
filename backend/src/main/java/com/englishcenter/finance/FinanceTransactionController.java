package com.englishcenter.finance;

import com.englishcenter.common.api.ApiResponse;
import com.englishcenter.common.api.PageMeta;
import com.englishcenter.finance.dto.CancelTransactionRequest;
import com.englishcenter.finance.dto.CashTransactionResponse;
import com.englishcenter.finance.dto.ManualExpenseRequest;
import com.englishcenter.finance.dto.ManualIncomeRequest;
import com.englishcenter.finance.dto.TransferRequest;
import com.englishcenter.finance.dto.TransferResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/finance/transactions")
public class FinanceTransactionController {
    private final FinancePostingService financePostingService;
    private final FinanceCalculationService financeCalculationService;

    public FinanceTransactionController(
            FinancePostingService financePostingService,
            FinanceCalculationService financeCalculationService
    ) {
        this.financePostingService = financePostingService;
        this.financeCalculationService = financeCalculationService;
    }

    @GetMapping
    public ApiResponse<List<CashTransactionResponse>> search(
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) TransactionDirection direction,
            @RequestParam(required = false) TransactionSourceType sourceType,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        LocalDate effectiveFrom = fromDate;
        LocalDate effectiveTo = toDate;
        if (year != null && month != null) {
            java.time.YearMonth ym = java.time.YearMonth.of(year, month);
            effectiveFrom = ym.atDay(1);
            effectiveTo = ym.atEndOfMonth();
        }

        Page<CashTransactionResponse> result = financeCalculationService.searchTransactions(
                effectiveFrom, effectiveTo, accountId, categoryId, direction, sourceType, status, keyword, page, size
        );
        PageMeta meta = new PageMeta(result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
        return ApiResponse.success(result.getContent(), meta);
    }

    @GetMapping("/{id}")
    public ApiResponse<CashTransactionResponse> get(@PathVariable Long id) {
        return ApiResponse.success(financeCalculationService.getTransaction(id));
    }

    @PostMapping("/income")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CashTransactionResponse> postIncome(@Valid @RequestBody ManualIncomeRequest request) {
        return ApiResponse.success(financePostingService.postManualIncome(request));
    }

    @PostMapping("/expense")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CashTransactionResponse> postExpense(@Valid @RequestBody ManualExpenseRequest request) {
        return ApiResponse.success(financePostingService.postManualExpense(request));
    }

    @PostMapping("/transfer")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TransferResponse> transfer(@Valid @RequestBody TransferRequest request) {
        return ApiResponse.success(financePostingService.transfer(request));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<CashTransactionResponse> cancel(
            @PathVariable Long id,
            @Valid @RequestBody CancelTransactionRequest request
    ) {
        return ApiResponse.success(financePostingService.cancelTransaction(id, request));
    }

    /**
     * @deprecated Use {@code POST /{id}/cancel}. Kept temporarily for compatibility.
     */
    @Deprecated
    @PostMapping("/{id}/reverse")
    public ApiResponse<CashTransactionResponse> reverse(
            @PathVariable Long id,
            @Valid @RequestBody CancelTransactionRequest request
    ) {
        return ApiResponse.success(financePostingService.cancelTransaction(id, request));
    }
}
