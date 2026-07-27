package com.englishcenter.auth.dto;

public record CreateUserResponse(
        UserAccountResponse user,
        String temporaryPassword
) {
}
