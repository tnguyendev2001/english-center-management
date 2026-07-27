package com.englishcenter.finance;

import com.englishcenter.common.api.ApiResponse;
import com.englishcenter.finance.dto.AccountBalanceResponse;
import com.englishcenter.finance.dto.FinancialAccountRequest;
import com.englishcenter.finance.dto.FinancialAccountResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/finance/accounts")
@PreAuthorize("hasRole('ADMIN')")
public class FinanceAccountController {
    private final FinancialAccountService financialAccountService;
    private final FinanceCalculationService financeCalculationService;

    public FinanceAccountController(
            FinancialAccountService financialAccountService,
            FinanceCalculationService financeCalculationService
    ) {
        this.financialAccountService = financialAccountService;
        this.financeCalculationService = financeCalculationService;
    }

    @GetMapping
    public ApiResponse<List<FinancialAccountResponse>> list() {
        return ApiResponse.success(financialAccountService.listAccounts());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FinancialAccountResponse> create(@Valid @RequestBody FinancialAccountRequest request) {
        return ApiResponse.success(financialAccountService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<FinancialAccountResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody FinancialAccountRequest request
    ) {
        return ApiResponse.success(financialAccountService.update(id, request));
    }

    @PatchMapping("/{id}/activate")
    public ApiResponse<FinancialAccountResponse> activate(@PathVariable Long id) {
        return ApiResponse.success(financialAccountService.activate(id));
    }

    @PatchMapping("/{id}/deactivate")
    public ApiResponse<FinancialAccountResponse> deactivate(@PathVariable Long id) {
        return ApiResponse.success(financialAccountService.deactivate(id));
    }

    @GetMapping("/{id}/balance")
    public ApiResponse<AccountBalanceResponse> balance(
            @PathVariable Long id,
            @RequestParam(required = false) LocalDate asOfDate
    ) {
        return ApiResponse.success(financeCalculationService.getAccountBalance(id, asOfDate));
    }
}
