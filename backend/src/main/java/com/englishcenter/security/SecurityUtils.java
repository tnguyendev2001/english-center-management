package com.englishcenter.security;

import com.englishcenter.auth.security.AccountPrincipal;
import com.englishcenter.common.exception.BusinessException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {
    private SecurityUtils() {
    }

    public static AccountPrincipal requirePrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AccountPrincipal principal)) {
            throw new AccessDeniedException("Bạn không có quyền thực hiện chức năng này.");
        }
        return principal;
    }

    public static AccountPrincipal requirePrincipal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AccountPrincipal principal)) {
            throw new AccessDeniedException("Bạn không có quyền thực hiện chức năng này.");
        }
        return principal;
    }

    public static String currentUsernameOrSystem() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AccountPrincipal principal) {
            return principal.username();
        }
        return "system";
    }

    public static Long requireLinkedStudentId() {
        AccountPrincipal principal = requirePrincipal();
        if (principal.studentId() == null) {
            throw new BusinessException("Tài khoản không liên kết với học viên.");
        }
        return principal.studentId();
    }

    public static Long requireLinkedTeacherId() {
        AccountPrincipal principal = requirePrincipal();
        if (principal.teacherId() == null) {
            throw new BusinessException("Tài khoản không liên kết với giáo viên.");
        }
        return principal.teacherId();
    }
}
