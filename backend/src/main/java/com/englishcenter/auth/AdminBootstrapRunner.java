package com.englishcenter.auth;

import com.englishcenter.common.config.AdminBootstrapProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminBootstrapRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final UserAccountRepository userAccountRepository;
    private final AdminBootstrapProperties adminBootstrapProperties;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final AuthAuditLogger authAuditLogger;

    public AdminBootstrapRunner(
            UserAccountRepository userAccountRepository,
            AdminBootstrapProperties adminBootstrapProperties,
            PasswordEncoder passwordEncoder,
            PasswordPolicy passwordPolicy,
            AuthAuditLogger authAuditLogger
    ) {
        this.userAccountRepository = userAccountRepository;
        this.adminBootstrapProperties = adminBootstrapProperties;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
        this.authAuditLogger = authAuditLogger;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userAccountRepository.existsByRole(AccountRole.ADMIN)) {
            return;
        }

        String username = adminBootstrapProperties.getUsername();
        String password = adminBootstrapProperties.getPassword();
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            log.warn("No ADMIN account exists. Set APP_ADMIN_USERNAME and APP_ADMIN_PASSWORD to bootstrap one.");
            return;
        }

        passwordPolicy.validate(password);

        UserAccount admin = new UserAccount();
        admin.setUsername(username.trim());
        admin.setNormalizedUsername(AuthService.normalizeUsername(username));
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRole(AccountRole.ADMIN);
        admin.setStatus(AccountStatus.ACTIVE);
        admin.setMustChangePassword(true);
        admin.setFailedLoginAttempts(0);
        admin.setTokenVersion(0);
        admin.setCreatedBy("bootstrap");
        admin.setUpdatedBy("bootstrap");

        UserAccount saved = userAccountRepository.save(admin);
        authAuditLogger.accountCreated(saved.getId(), saved.getUsername(), AccountRole.ADMIN, "bootstrap");
        log.info("Bootstrapped initial ADMIN account username={}", saved.getUsername());
    }
}
