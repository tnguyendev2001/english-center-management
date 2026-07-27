package com.englishcenter.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AuthAuditLogger {
    private static final Logger log = LoggerFactory.getLogger(AuthAuditLogger.class);

    public void loginSuccess(Long accountId, String username) {
        log.info("AUTH_AUDIT event=LOGIN_SUCCESS accountId={} username={}", accountId, username);
    }

    public void loginFailed(String username, String reason) {
        log.info("AUTH_AUDIT event=LOGIN_FAILED username={} reason={}", username, reason);
    }

    public void accountLocked(Long accountId, String username) {
        log.info("AUTH_AUDIT event=ACCOUNT_LOCKED accountId={} username={}", accountId, username);
    }

    public void accountCreated(Long accountId, String username, AccountRole role, String actor) {
        log.info(
                "AUTH_AUDIT event=ACCOUNT_CREATED accountId={} username={} role={} actor={}",
                accountId,
                username,
                role,
                actor
        );
    }

    public void accountEnabled(Long accountId, String username, String actor) {
        log.info("AUTH_AUDIT event=ACCOUNT_ENABLED accountId={} username={} actor={}", accountId, username, actor);
    }

    public void accountDisabled(Long accountId, String username, String actor) {
        log.info("AUTH_AUDIT event=ACCOUNT_DISABLED accountId={} username={} actor={}", accountId, username, actor);
    }

    public void passwordReset(Long accountId, String username, String actor) {
        log.info("AUTH_AUDIT event=PASSWORD_RESET accountId={} username={} actor={}", accountId, username, actor);
    }

    public void passwordChanged(Long accountId, String username) {
        log.info("AUTH_AUDIT event=PASSWORD_CHANGED accountId={} username={}", accountId, username);
    }

    public void roleChanged(Long accountId, String username, AccountRole from, AccountRole to, String actor) {
        log.info(
                "AUTH_AUDIT event=ROLE_CHANGED accountId={} username={} from={} to={} actor={}",
                accountId,
                username,
                from,
                to,
                actor
        );
    }

    public void profileLinkChanged(Long accountId, String username, String actor) {
        log.info(
                "AUTH_AUDIT event=PROFILE_LINK_CHANGED accountId={} username={} actor={}",
                accountId,
                username,
                actor
        );
    }
}
