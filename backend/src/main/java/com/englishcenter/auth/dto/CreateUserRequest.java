package com.englishcenter.auth.dto;

import com.englishcenter.auth.AccountRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "Tên đăng nhập là bắt buộc")
        @Size(max = 100, message = "Tên đăng nhập không được vượt quá 100 ký tự")
        String username,

        @NotNull(message = "Vai trò là bắt buộc")
        AccountRole role,

        Long studentId,

        Long teacherId
) {
}
