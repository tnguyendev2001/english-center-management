package com.englishcenter.studentpackage;

import com.englishcenter.common.api.ApiResponse;
import com.englishcenter.enrollment.EnrollmentProgressService;
import com.englishcenter.enrollment.dto.EnrollmentLearningProgressResponse;
import com.englishcenter.packagechange.PackageChangeService;
import com.englishcenter.packagechange.dto.ChangePackagePreviewRequest;
import com.englishcenter.packagechange.dto.ChangePackagePreviewResponse;
import com.englishcenter.packagechange.dto.ChangePackageRequest;
import com.englishcenter.packagechange.dto.ChangePackageResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StudentPackageController {
    private final EnrollmentProgressService enrollmentProgressService;
    private final PackageChangeService packageChangeService;

    public StudentPackageController(
            EnrollmentProgressService enrollmentProgressService,
            PackageChangeService packageChangeService
    ) {
        this.enrollmentProgressService = enrollmentProgressService;
        this.packageChangeService = packageChangeService;
    }

    @GetMapping("/api/students/{studentId}/packages")
    public ApiResponse<List<EnrollmentLearningProgressResponse>> getByStudentId(@PathVariable Long studentId) {
        return ApiResponse.success(enrollmentProgressService.getByStudentId(studentId));
    }

    @GetMapping("/api/classrooms/{classroomId}/student-packages")
    public ApiResponse<List<EnrollmentLearningProgressResponse>> getByClassroomId(@PathVariable Long classroomId) {
        return ApiResponse.success(enrollmentProgressService.getByClassroomId(classroomId));
    }

    @PostMapping("/api/student-packages/{studentPackageId}/change-package/preview")
    public ApiResponse<ChangePackagePreviewResponse> previewChangePackage(
            @PathVariable Long studentPackageId,
            @Valid @RequestBody ChangePackagePreviewRequest request
    ) {
        return ApiResponse.success(packageChangeService.preview(
                studentPackageId,
                request.newTuitionPackageId(),
                request.changeMode()
        ));
    }

    @PostMapping("/api/student-packages/{studentPackageId}/change-package")
    public ApiResponse<ChangePackageResponse> changePackage(
            @PathVariable Long studentPackageId,
            @Valid @RequestBody ChangePackageRequest request
    ) {
        return ApiResponse.success(packageChangeService.changePackage(studentPackageId, request));
    }
}
