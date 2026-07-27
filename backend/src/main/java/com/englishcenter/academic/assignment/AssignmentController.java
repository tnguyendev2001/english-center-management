package com.englishcenter.academic.assignment;

import com.englishcenter.academic.assignment.dto.AssignmentResponse;
import com.englishcenter.academic.assignment.dto.CreateAssignmentRequest;
import com.englishcenter.academic.assignment.dto.UpdateAssignmentRequest;
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
@RequestMapping("/api/academic/assignments")
public class AssignmentController {
    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<List<AssignmentResponse>> search(
            @RequestParam(required = false) Long classroomId,
            @RequestParam(required = false) AssignmentStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<AssignmentResponse> result = assignmentService.search(classroomId, status, keyword, page, size);
        return ApiResponse.success(
                result.getContent(),
                new PageMeta(result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages())
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<AssignmentResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(assignmentService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<AssignmentResponse> create(@Valid @RequestBody CreateAssignmentRequest request) {
        return ApiResponse.success(assignmentService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<AssignmentResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAssignmentRequest request
    ) {
        return ApiResponse.success(assignmentService.update(id, request));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<AssignmentResponse> publish(@PathVariable Long id) {
        return ApiResponse.success(assignmentService.publish(id));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<AssignmentResponse> close(@PathVariable Long id) {
        return ApiResponse.success(assignmentService.close(id));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<AssignmentResponse> cancel(@PathVariable Long id) {
        return ApiResponse.success(assignmentService.cancel(id));
    }
}
