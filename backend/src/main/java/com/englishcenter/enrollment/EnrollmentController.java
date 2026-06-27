package com.englishcenter.enrollment;

import com.englishcenter.common.api.ApiResponse;
import com.englishcenter.common.api.PageMeta;
import com.englishcenter.enrollment.dto.EnrollStudentRequest;
import com.englishcenter.enrollment.dto.EnrollmentResponse;
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
}
