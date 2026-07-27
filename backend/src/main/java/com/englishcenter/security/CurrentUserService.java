package com.englishcenter.security;

import com.englishcenter.auth.AccountRole;
import com.englishcenter.auth.security.AccountPrincipal;
import com.englishcenter.common.exception.BusinessException;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {
    public AccountPrincipal getCurrentUser() {
        return SecurityUtils.requirePrincipal();
    }

    public Long requireCurrentTeacherId() {
        AccountPrincipal principal = getCurrentUser();
        if (principal.role() != AccountRole.TEACHER || principal.teacherId() == null) {
            throw new BusinessException("Tài khoản không liên kết với giáo viên.");
        }
        return principal.teacherId();
    }

    public Long requireCurrentStudentId() {
        AccountPrincipal principal = getCurrentUser();
        if (principal.role() != AccountRole.STUDENT || principal.studentId() == null) {
            throw new BusinessException("Tài khoản không liên kết với học viên.");
        }
        return principal.studentId();
    }
}
