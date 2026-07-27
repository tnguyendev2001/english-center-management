package com.englishcenter.academic.submission;

import com.englishcenter.academic.submission.dto.AssignmentSubmissionResponse;
import com.englishcenter.academic.submission.dto.GradeSubmissionRequest;
import com.englishcenter.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/academic")
public class SubmissionController {
    private final AssignmentSubmissionService assignmentSubmissionService;

    public SubmissionController(AssignmentSubmissionService assignmentSubmissionService) {
        this.assignmentSubmissionService = assignmentSubmissionService;
    }

    @GetMapping("/assignments/{id}/submissions")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<List<AssignmentSubmissionResponse>> listByAssignment(@PathVariable Long id) {
        return ApiResponse.success(assignmentSubmissionService.listByAssignment(id));
    }

    @PutMapping("/submissions/{id}/grade")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<AssignmentSubmissionResponse> grade(
            @PathVariable Long id,
            @Valid @RequestBody GradeSubmissionRequest request
    ) {
        return ApiResponse.success(assignmentSubmissionService.grade(id, request));
    }
}
