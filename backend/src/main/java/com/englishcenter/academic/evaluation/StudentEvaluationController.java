package com.englishcenter.academic.evaluation;

import com.englishcenter.academic.evaluation.dto.ReopenEvaluationRequest;
import com.englishcenter.academic.evaluation.dto.StudentEvaluationResponse;
import com.englishcenter.academic.evaluation.dto.UpsertStudentEvaluationRequest;
import com.englishcenter.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/academic/evaluations")
public class StudentEvaluationController {
    private final StudentEvaluationService studentEvaluationService;

    public StudentEvaluationController(StudentEvaluationService studentEvaluationService) {
        this.studentEvaluationService = studentEvaluationService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<List<StudentEvaluationResponse>> list(
            @RequestParam Long periodId,
            @RequestParam Long classroomId
    ) {
        return ApiResponse.success(studentEvaluationService.list(periodId, classroomId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    public ApiResponse<StudentEvaluationResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(studentEvaluationService.getById(id));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<StudentEvaluationResponse> upsert(@Valid @RequestBody UpsertStudentEvaluationRequest request) {
        return ApiResponse.success(studentEvaluationService.upsert(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<StudentEvaluationResponse> upsertById(
            @PathVariable Long id,
            @Valid @RequestBody UpsertStudentEvaluationRequest request
    ) {
        return ApiResponse.success(studentEvaluationService.upsert(request));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<StudentEvaluationResponse> publish(@PathVariable Long id) {
        return ApiResponse.success(studentEvaluationService.publish(id));
    }

    @PostMapping("/{id}/finalize")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<StudentEvaluationResponse> finalize(@PathVariable Long id) {
        return ApiResponse.success(studentEvaluationService.finalize(id));
    }

    @PostMapping("/{id}/reopen")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<StudentEvaluationResponse> reopen(
            @PathVariable Long id,
            @Valid @RequestBody ReopenEvaluationRequest request
    ) {
        return ApiResponse.success(studentEvaluationService.reopen(id, request));
    }
}
