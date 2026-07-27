package com.englishcenter.auth;

import com.englishcenter.auth.dto.AuthUserResponse;
import com.englishcenter.auth.dto.ChangePasswordRequest;
import com.englishcenter.auth.dto.LoginRequest;
import com.englishcenter.auth.dto.LoginResponse;
import com.englishcenter.auth.security.AccountPrincipal;
import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.security.SecurityUtils;
import java.time.LocalDateTime;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private static final String GENERIC_LOGIN_ERROR = "Tên đăng nhập hoặc mật khẩu không đúng.";

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final PasswordPolicy passwordPolicy;
    private final UserAccountMapper userAccountMapper;
    private final AuthAuditLogger authAuditLogger;
    private final LoginAttemptService loginAttemptService;

    public AuthService(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            PasswordPolicy passwordPolicy,
            UserAccountMapper userAccountMapper,
            AuthAuditLogger authAuditLogger,
            LoginAttemptService loginAttemptService
    ) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.passwordPolicy = passwordPolicy;
        this.userAccountMapper = userAccountMapper;
        this.authAuditLogger = authAuditLogger;
        this.loginAttemptService = loginAttemptService;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String normalizedUsername = normalizeUsername(request.username());
        UserAccount account = userAccountRepository.findByNormalizedUsername(normalizedUsername).orElse(null);

        if (account == null) {
            authAuditLogger.loginFailed(normalizedUsername, "USER_NOT_FOUND");
            throw new BadCredentialsException(GENERIC_LOGIN_ERROR);
        }

        if (account.getStatus() == AccountStatus.DISABLED) {
            authAuditLogger.loginFailed(account.getUsername(), "DISABLED");
            throw new BadCredentialsException("Tài khoản đã bị vô hiệu hóa. Vui lòng liên hệ quản trị viên.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (account.getLockedUntil() != null && account.getLockedUntil().isAfter(now)) {
            loginAttemptService.markLocked(account.getId());
            authAuditLogger.loginFailed(account.getUsername(), "LOCKED");
            throw new BadCredentialsException(
                    "Tài khoản đang tạm khóa do đăng nhập sai nhiều lần. Vui lòng thử lại sau."
            );
        }

        if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            loginAttemptService.registerFailedLogin(account.getId());
            authAuditLogger.loginFailed(account.getUsername(), "BAD_PASSWORD");
            throw new BadCredentialsException(GENERIC_LOGIN_ERROR);
        }

        account.setFailedLoginAttempts(0);
        account.setLockedUntil(null);
        if (account.getStatus() == AccountStatus.LOCKED) {
            account.setStatus(AccountStatus.ACTIVE);
        }
        account.setLastLoginAt(now);
        userAccountRepository.save(account);

        JwtTokenService.IssuedToken token = jwtTokenService.issueToken(account);
        authAuditLogger.loginSuccess(account.getId(), account.getUsername());

        return new LoginResponse(
                token.accessToken(),
                "Bearer",
                token.expiresInSeconds(),
                userAccountMapper.toAuthUser(account)
        );
    }

    @Transactional(readOnly = true)
    public AuthUserResponse me() {
        AccountPrincipal principal = SecurityUtils.requirePrincipal();
        UserAccount account = userAccountRepository.findById(principal.id())
                .orElseThrow(() -> new BusinessException("Tài khoản không tồn tại."));
        return userAccountMapper.toAuthUser(account);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        AccountPrincipal principal = SecurityUtils.requirePrincipal();
        UserAccount account = userAccountRepository.findById(principal.id())
                .orElseThrow(() -> new BusinessException("Tài khoản không tồn tại."));

        if (!passwordEncoder.matches(request.currentPassword(), account.getPasswordHash())) {
            throw new BusinessException("Mật khẩu hiện tại không đúng.");
        }
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BusinessException("Xác nhận mật khẩu không khớp.");
        }
        passwordPolicy.validate(request.newPassword());
        if (passwordEncoder.matches(request.newPassword(), account.getPasswordHash())) {
            throw new BusinessException("Mật khẩu mới phải khác mật khẩu hiện tại.");
        }

        account.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        account.setMustChangePassword(false);
        account.setPasswordChangedAt(LocalDateTime.now());
        account.incrementTokenVersion();
        account.setUpdatedBy(principal.username());
        userAccountRepository.save(account);
        authAuditLogger.passwordChanged(account.getId(), account.getUsername());
    }

    public static String normalizeUsername(String username) {
        if (username == null) {
            return "";
        }
        return username.trim().toLowerCase();
    }
}
