package com.englishcenter.auth;

import com.englishcenter.common.exception.BusinessException;
import org.springframework.stereotype.Component;

@Component
public class PasswordPolicy {
    public void validate(String password) {
        if (password == null || password.isBlank()) {
            throw new BusinessException("Mật khẩu không được để trống.");
        }
        if (password.length() < 8) {
            throw new BusinessException("Mật khẩu phải có ít nhất 8 ký tự.");
        }
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw new BusinessException("Mật khẩu phải chứa ít nhất một chữ cái và một chữ số.");
        }
    }
}
