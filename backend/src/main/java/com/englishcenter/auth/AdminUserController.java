package com.englishcenter.auth;

import com.englishcenter.auth.dto.CreateUserRequest;
import com.englishcenter.auth.dto.CreateUserResponse;
import com.englishcenter.auth.dto.ResetPasswordResponse;
import com.englishcenter.auth.dto.UserAccountResponse;
import com.englishcenter.common.api.ApiResponse;
import com.englishcenter.common.api.PageMeta;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {
    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public ApiResponse<List<UserAccountResponse>> search(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) AccountRole role,
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<UserAccountResponse> users = adminUserService.search(username, role, status, page, size);
        PageMeta meta = new PageMeta(
                users.getNumber(),
                users.getSize(),
                users.getTotalElements(),
                users.getTotalPages()
        );
        return ApiResponse.success(users.getContent(), meta);
    }

    @GetMapping("/{id}")
    public ApiResponse<UserAccountResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(adminUserService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CreateUserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.success(adminUserService.create(request));
    }

    @PatchMapping("/{id}/enable")
    public ApiResponse<UserAccountResponse> enable(@PathVariable Long id) {
        return ApiResponse.success(adminUserService.enable(id));
    }

    @PatchMapping("/{id}/disable")
    public ApiResponse<UserAccountResponse> disable(@PathVariable Long id) {
        return ApiResponse.success(adminUserService.disable(id));
    }

    @PostMapping("/{id}/reset-password")
    public ApiResponse<ResetPasswordResponse> resetPassword(@PathVariable Long id) {
        return ApiResponse.success(adminUserService.resetPassword(id));
    }
}
