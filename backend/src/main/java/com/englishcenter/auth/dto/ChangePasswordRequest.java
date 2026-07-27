package com.englishcenter.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @NotBlank(message = "Mật khẩu hiện tại là bắt buộc")
        String currentPassword,

        @NotBlank(message = "Mật khẩu mới là bắt buộc")
        String newPassword,

        @NotBlank(message = "Xác nhận mật khẩu là bắt buộc")
        String confirmPassword
) {
}
