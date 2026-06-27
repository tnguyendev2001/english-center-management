package com.englishcenter.tuitionpackage;

import com.englishcenter.common.api.ApiResponse;
import com.englishcenter.common.api.PageMeta;
import com.englishcenter.tuitionpackage.dto.TuitionPackageCreateRequest;
import com.englishcenter.tuitionpackage.dto.TuitionPackageResponse;
import com.englishcenter.tuitionpackage.dto.TuitionPackageUpdateRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tuition-packages")
public class TuitionPackageController {
    private final TuitionPackageService tuitionPackageService;

    public TuitionPackageController(TuitionPackageService tuitionPackageService) {
        this.tuitionPackageService = tuitionPackageService;
    }

    @GetMapping
    public ApiResponse<List<TuitionPackageResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<TuitionPackageResponse> tuitionPackages = tuitionPackageService.search(keyword, page, size);
        PageMeta meta = new PageMeta(
                tuitionPackages.getNumber(),
                tuitionPackages.getSize(),
                tuitionPackages.getTotalElements(),
                tuitionPackages.getTotalPages()
        );

        return ApiResponse.success(tuitionPackages.getContent(), meta);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TuitionPackageResponse> create(
            @Valid @RequestBody TuitionPackageCreateRequest request
    ) {
        return ApiResponse.success(tuitionPackageService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<TuitionPackageResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(tuitionPackageService.getById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<TuitionPackageResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody TuitionPackageUpdateRequest request
    ) {
        return ApiResponse.success(tuitionPackageService.update(id, request));
    }

    @PostMapping("/{id}/deactivate")
    public ApiResponse<TuitionPackageResponse> deactivate(@PathVariable Long id) {
        return ApiResponse.success(tuitionPackageService.deactivate(id));
    }
}
