package com.englishcenter.finance;

import com.englishcenter.common.api.ApiResponse;
import com.englishcenter.finance.dto.TransactionCategoryRequest;
import com.englishcenter.finance.dto.TransactionCategoryResponse;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/finance/categories")
@PreAuthorize("hasRole('ADMIN')")
public class FinanceCategoryController {
    private final TransactionCategoryService transactionCategoryService;

    public FinanceCategoryController(TransactionCategoryService transactionCategoryService) {
        this.transactionCategoryService = transactionCategoryService;
    }

    @GetMapping
    public ApiResponse<List<TransactionCategoryResponse>> list() {
        return ApiResponse.success(transactionCategoryService.listCategories());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TransactionCategoryResponse> create(@Valid @RequestBody TransactionCategoryRequest request) {
        return ApiResponse.success(transactionCategoryService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<TransactionCategoryResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody TransactionCategoryRequest request
    ) {
        return ApiResponse.success(transactionCategoryService.update(id, request));
    }

    @PatchMapping("/{id}/activate")
    public ApiResponse<TransactionCategoryResponse> activate(@PathVariable Long id) {
        return ApiResponse.success(transactionCategoryService.activate(id));
    }

    @PatchMapping("/{id}/deactivate")
    public ApiResponse<TransactionCategoryResponse> deactivate(@PathVariable Long id) {
        return ApiResponse.success(transactionCategoryService.deactivate(id));
    }
}
