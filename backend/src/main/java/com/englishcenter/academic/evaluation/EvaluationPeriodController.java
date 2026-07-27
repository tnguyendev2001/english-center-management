package com.englishcenter.academic.evaluation;

import com.englishcenter.academic.evaluation.dto.CreateEvaluationPeriodRequest;
import com.englishcenter.academic.evaluation.dto.EvaluationPeriodResponse;
import com.englishcenter.academic.evaluation.dto.ReopenEvaluationRequest;
import com.englishcenter.academic.evaluation.dto.UpdateEvaluationPeriodRequest;
import com.englishcenter.common.api.ApiResponse;
import com.englishcenter.common.api.PageMeta;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/academic/evaluation-periods")
public class EvaluationPeriodController {
    private final EvaluationPeriodService evaluationPeriodService;

    public EvaluationPeriodController(EvaluationPeriodService evaluationPeriodService) {
        this.evaluationPeriodService = evaluationPeriodService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<List<EvaluationPeriodResponse>> search(
            @RequestParam(required = false) Long classroomId,
            @RequestParam(required = false) EvaluationPeriodStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<EvaluationPeriodResponse> result =
                evaluationPeriodService.search(classroomId, status, keyword, page, size);
        return ApiResponse.success(
                result.getContent(),
                new PageMeta(result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages())
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<EvaluationPeriodResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(evaluationPeriodService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<EvaluationPeriodResponse> create(@Valid @RequestBody CreateEvaluationPeriodRequest request) {
        return ApiResponse.success(evaluationPeriodService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<EvaluationPeriodResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEvaluationPeriodRequest request
    ) {
        return ApiResponse.success(evaluationPeriodService.update(id, request));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<EvaluationPeriodResponse> close(@PathVariable Long id) {
        return ApiResponse.success(evaluationPeriodService.close(id));
    }

    @PostMapping("/{id}/reopen")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<EvaluationPeriodResponse> reopen(
            @PathVariable Long id,
            @Valid @RequestBody ReopenEvaluationRequest request
    ) {
        return ApiResponse.success(evaluationPeriodService.reopen(id, request));
    }
}
