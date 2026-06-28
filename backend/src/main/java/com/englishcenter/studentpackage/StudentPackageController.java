package com.englishcenter.studentpackage;

import com.englishcenter.common.api.ApiResponse;
import com.englishcenter.packagechange.PackageChangeService;
import com.englishcenter.packagechange.dto.ChangePackagePreviewRequest;
import com.englishcenter.packagechange.dto.ChangePackagePreviewResponse;
import com.englishcenter.packagechange.dto.ChangePackageRequest;
import com.englishcenter.packagechange.dto.ChangePackageResponse;
import com.englishcenter.studentpackage.dto.StudentPackageProgressResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StudentPackageController {
    private final StudentPackageService studentPackageService;
    private final PackageChangeService packageChangeService;

    public StudentPackageController(
            StudentPackageService studentPackageService,
            PackageChangeService packageChangeService
    ) {
        this.studentPackageService = studentPackageService;
        this.packageChangeService = packageChangeService;
    }

    @GetMapping("/api/students/{studentId}/packages")
    public ApiResponse<List<StudentPackageProgressResponse>> getByStudentId(@PathVariable Long studentId) {
        return ApiResponse.success(studentPackageService.getByStudentId(studentId));
    }

    @GetMapping("/api/classrooms/{classroomId}/student-packages")
    public ApiResponse<List<StudentPackageProgressResponse>> getByClassroomId(@PathVariable Long classroomId) {
        return ApiResponse.success(studentPackageService.getByClassroomId(classroomId));
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
