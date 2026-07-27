package com.englishcenter.auth.dto;

public record ResetPasswordResponse(
        UserAccountResponse user,
        String temporaryPassword
) {
}
