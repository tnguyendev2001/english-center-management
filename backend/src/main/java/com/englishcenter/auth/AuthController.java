package com.englishcenter.auth;

import com.englishcenter.auth.dto.AuthUserResponse;
import com.englishcenter.auth.dto.ChangePasswordRequest;
import com.englishcenter.auth.dto.LoginRequest;
import com.englishcenter.auth.dto.LoginResponse;
import com.englishcenter.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<AuthUserResponse> me() {
        return ApiResponse.success(authService.me());
    }

    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ApiResponse.success(null);
    }
}
