package com.englishcenter.academic.assessment;

import com.englishcenter.academic.assessment.dto.AssessmentResponse;
import com.englishcenter.academic.assessment.dto.CreateAssessmentRequest;
import com.englishcenter.academic.assessment.dto.UpdateAssessmentRequest;
import com.englishcenter.academic.score.AssessmentScoreService;
import com.englishcenter.academic.score.dto.AssessmentScoreResponse;
import com.englishcenter.academic.score.dto.BulkScoreUpdateRequest;
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
@RequestMapping("/api/academic/assessments")
public class AssessmentController {
    private final AssessmentService assessmentService;
    private final AssessmentScoreService assessmentScoreService;

    public AssessmentController(
            AssessmentService assessmentService,
            AssessmentScoreService assessmentScoreService
    ) {
        this.assessmentService = assessmentService;
        this.assessmentScoreService = assessmentScoreService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<List<AssessmentResponse>> search(
            @RequestParam(required = false) Long classroomId,
            @RequestParam(required = false) AssessmentStatus status,
            @RequestParam(required = false) Long periodId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<AssessmentResponse> result = assessmentService.search(classroomId, status, periodId, page, size);
        return ApiResponse.success(
                result.getContent(),
                new PageMeta(result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages())
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<AssessmentResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(assessmentService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<AssessmentResponse> create(@Valid @RequestBody CreateAssessmentRequest request) {
        return ApiResponse.success(assessmentService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<AssessmentResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAssessmentRequest request
    ) {
        return ApiResponse.success(assessmentService.update(id, request));
    }

    @PostMapping("/{id}/open")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<AssessmentResponse> open(@PathVariable Long id) {
        return ApiResponse.success(assessmentService.open(id));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<AssessmentResponse> complete(@PathVariable Long id) {
        return ApiResponse.success(assessmentService.complete(id));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<AssessmentResponse> cancel(@PathVariable Long id) {
        return ApiResponse.success(assessmentService.cancel(id));
    }

    @GetMapping("/{id}/scores")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<List<AssessmentScoreResponse>> listScores(@PathVariable Long id) {
        return ApiResponse.success(assessmentScoreService.listByAssessment(id));
    }

    @PutMapping("/{id}/scores")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<List<AssessmentScoreResponse>> bulkUpdateScores(
            @PathVariable Long id,
            @Valid @RequestBody BulkScoreUpdateRequest request
    ) {
        return ApiResponse.success(assessmentScoreService.bulkUpdate(id, request));
    }

    @PostMapping("/{id}/publish-scores")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<List<AssessmentScoreResponse>> publishScores(@PathVariable Long id) {
        return ApiResponse.success(assessmentScoreService.publishScores(id));
    }
}
