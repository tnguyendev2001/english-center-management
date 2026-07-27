package com.englishcenter.auth;

import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginAttemptService {
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 15;

    private final UserAccountRepository userAccountRepository;
    private final AuthAuditLogger authAuditLogger;

    public LoginAttemptService(
            UserAccountRepository userAccountRepository,
            AuthAuditLogger authAuditLogger
    ) {
        this.userAccountRepository = userAccountRepository;
        this.authAuditLogger = authAuditLogger;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registerFailedLogin(Long accountId) {
        UserAccount account = userAccountRepository.findById(accountId).orElse(null);
        if (account == null) {
            return;
        }
        account.setFailedLoginAttempts(account.getFailedLoginAttempts() + 1);
        if (account.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
            account.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
            account.setStatus(AccountStatus.LOCKED);
            authAuditLogger.accountLocked(account.getId(), account.getUsername());
        }
        userAccountRepository.save(account);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markLocked(Long accountId) {
        UserAccount account = userAccountRepository.findById(accountId).orElse(null);
        if (account == null) {
            return;
        }
        if (account.getStatus() != AccountStatus.LOCKED) {
            account.setStatus(AccountStatus.LOCKED);
            userAccountRepository.save(account);
        }
    }
}
