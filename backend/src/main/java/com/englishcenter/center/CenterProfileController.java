package com.englishcenter.center;

import com.englishcenter.center.dto.CenterProfileResponse;
import com.englishcenter.center.dto.UpdateCenterProfileRequest;
import com.englishcenter.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/center-profile")
public class CenterProfileController {
    private final CenterProfileService centerProfileService;

    public CenterProfileController(CenterProfileService centerProfileService) {
        this.centerProfileService = centerProfileService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    public ApiResponse<CenterProfileResponse> getProfile() {
        return ApiResponse.success(centerProfileService.getProfile());
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CenterProfileResponse> updateProfile(@Valid @RequestBody UpdateCenterProfileRequest request) {
        return ApiResponse.success(centerProfileService.updateProfile(request));
    }
}
