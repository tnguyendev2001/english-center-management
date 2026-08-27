package com.englishcenter.enrollment;

import com.englishcenter.common.api.ApiResponse;
import com.englishcenter.common.api.PageMeta;
import com.englishcenter.enrollment.dto.EnrollStudentRequest;
import com.englishcenter.enrollment.dto.CancelEnrollmentRequest;
import com.englishcenter.enrollment.dto.EnrollmentResponse;
import com.englishcenter.enrollment.dto.DuplicateEnrollmentGroupResponse;
import com.englishcenter.enrollment.dto.EnrollmentLifecycleContextResponse;
import com.englishcenter.enrollment.dto.EnrollmentStatusHistoryResponse;
import com.englishcenter.enrollment.dto.HoldEnrollmentRequest;
import com.englishcenter.enrollment.dto.PauseEnrollmentRequest;
import com.englishcenter.enrollment.dto.ChangeLearningStartDateRequest;
import com.englishcenter.enrollment.dto.ReactivateEnrollmentRequest;
import com.englishcenter.enrollment.dto.StopEnrollmentRequest;
import com.englishcenter.enrollment.dto.TransferEnrollmentRequest;
import com.englishcenter.enrollment.dto.TransferEnrollmentResponse;
import jakarta.validation.Valid;
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
@RequestMapping("/api/enrollments")
public class EnrollmentController {
    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<EnrollmentResponse> enrollStudent(@Valid @RequestBody EnrollStudentRequest request) {
        return ApiResponse.success(enrollmentService.enrollStudent(request));
    }

    @GetMapping
    public ApiResponse<List<EnrollmentResponse>> getEnrollments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<EnrollmentResponse> enrollments = enrollmentService.getEnrollments(page, size);
        PageMeta meta = new PageMeta(
                enrollments.getNumber(),
                enrollments.getSize(),
                enrollments.getTotalElements(),
                enrollments.getTotalPages()
        );

        return ApiResponse.success(enrollments.getContent(), meta);
    }

    @GetMapping("/{id}")
    public ApiResponse<EnrollmentResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(enrollmentService.getById(id));
    }

    @GetMapping("/{id}/status-history")
    public ApiResponse<List<EnrollmentStatusHistoryResponse>> getStatusHistory(@PathVariable Long id) {
        return ApiResponse.success(enrollmentService.getStatusHistory(id));
    }

    @GetMapping("/{id}/lifecycle-context")
    public ApiResponse<EnrollmentLifecycleContextResponse> getLifecycleContext(@PathVariable Long id) {
        return ApiResponse.success(enrollmentService.getLifecycleContext(id));
    }

    @GetMapping("/duplicates")
    public ApiResponse<List<DuplicateEnrollmentGroupResponse>> getDuplicates() {
        return ApiResponse.success(enrollmentService.getDuplicates());
    }

    @PostMapping("/{id}/pause")
    public ApiResponse<EnrollmentResponse> pause(
            @PathVariable Long id,
            @Valid @RequestBody PauseEnrollmentRequest request
    ) {
        return ApiResponse.success(enrollmentService.pause(id, request));
    }

    @PostMapping("/{id}/learning-start-date")
    public ApiResponse<EnrollmentResponse> changeLearningStartDate(
            @PathVariable Long id,
            @Valid @RequestBody ChangeLearningStartDateRequest request
    ) {
        return ApiResponse.success(enrollmentService.changeLearningStartDate(id, request));
    }

    @PostMapping("/{id}/hold")
    public ApiResponse<EnrollmentResponse> hold(
            @PathVariable Long id,
            @Valid @RequestBody HoldEnrollmentRequest request
    ) {
        return ApiResponse.success(enrollmentService.hold(id, request));
    }

    @PostMapping("/{id}/reactivate")
    public ApiResponse<EnrollmentResponse> reactivate(
            @PathVariable Long id,
            @Valid @RequestBody ReactivateEnrollmentRequest request
    ) {
        return ApiResponse.success(enrollmentService.reactivate(id, request));
    }

    @PostMapping("/{id}/stop")
    public ApiResponse<EnrollmentResponse> stop(
            @PathVariable Long id,
            @Valid @RequestBody StopEnrollmentRequest request
    ) {
        return ApiResponse.success(enrollmentService.stop(id, request));
    }

    @PostMapping("/{id}/transfer")
    public ApiResponse<TransferEnrollmentResponse> transfer(
            @PathVariable Long id,
            @Valid @RequestBody TransferEnrollmentRequest request
    ) {
        return ApiResponse.success(enrollmentService.transfer(id, request));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<EnrollmentResponse> cancel(
            @PathVariable Long id,
            @Valid @RequestBody CancelEnrollmentRequest request
    ) {
        return ApiResponse.success(enrollmentService.cancel(id, request));
    }
}
