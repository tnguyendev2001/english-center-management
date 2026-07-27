package com.englishcenter.classpackage;

import com.englishcenter.classpackage.dto.AddClassPackageRequest;
import com.englishcenter.classpackage.dto.ClassPackageResponse;
import com.englishcenter.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/classrooms/{classroomId}/packages")
public class ClassPackageController {
    private final ClassPackageService classPackageService;

    public ClassPackageController(ClassPackageService classPackageService) {
        this.classPackageService = classPackageService;
    }

    @GetMapping
    @PreAuthorize("@authorizationService.canAccessClassroom(authentication, #classroomId)")
    public ApiResponse<List<ClassPackageResponse>> getActivePackages(@PathVariable Long classroomId) {
        return ApiResponse.success(classPackageService.getActivePackages(classroomId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ClassPackageResponse> addPackage(
            @PathVariable Long classroomId,
            @Valid @RequestBody AddClassPackageRequest request
    ) {
        return ApiResponse.success(classPackageService.addPackage(classroomId, request));
    }

    @PostMapping("/{tuitionPackageId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ClassPackageResponse> deactivatePackage(
            @PathVariable Long classroomId,
            @PathVariable Long tuitionPackageId
    ) {
        return ApiResponse.success(classPackageService.deactivatePackage(classroomId, tuitionPackageId));
    }
}
