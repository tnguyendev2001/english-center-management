package com.englishcenter.finance;

import com.englishcenter.common.api.ApiResponse;
import com.englishcenter.finance.dto.ClosePeriodRequest;
import com.englishcenter.finance.dto.FinancialPeriodResponse;
import com.englishcenter.finance.dto.ReopenPeriodRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/finance/periods")
@PreAuthorize("hasRole('ADMIN')")
public class FinancePeriodController {
    private final FinancialPeriodService financialPeriodService;

    public FinancePeriodController(FinancialPeriodService financialPeriodService) {
        this.financialPeriodService = financialPeriodService;
    }

    @GetMapping
    public ApiResponse<List<FinancialPeriodResponse>> list() {
        return ApiResponse.success(financialPeriodService.listPeriods());
    }

    @GetMapping("/{year}/{month}")
    public ApiResponse<FinancialPeriodResponse> get(@PathVariable int year, @PathVariable int month) {
        return ApiResponse.success(financialPeriodService.getPeriod(year, month));
    }

    @PostMapping("/{year}/{month}/close")
    public ApiResponse<FinancialPeriodResponse> close(
            @PathVariable int year,
            @PathVariable int month,
            @RequestBody(required = false) ClosePeriodRequest request
    ) {
        return ApiResponse.success(financialPeriodService.closePeriod(
                year,
                month,
                request != null ? request : new ClosePeriodRequest(null, false, null)
        ));
    }

    @PostMapping("/{year}/{month}/reopen")
    public ApiResponse<FinancialPeriodResponse> reopen(
            @PathVariable int year,
            @PathVariable int month,
            @Valid @RequestBody ReopenPeriodRequest request
    ) {
        return ApiResponse.success(financialPeriodService.reopenPeriod(year, month, request));
    }
}
