package com.englishcenter.auth.security;

import com.englishcenter.auth.AccountStatus;
import com.englishcenter.auth.UserAccount;
import com.englishcenter.auth.UserAccountRepository;
import java.time.LocalDateTime;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class AccountJwtAuthenticationConverter implements Converter<Jwt, UsernamePasswordAuthenticationToken> {
    private final UserAccountRepository userAccountRepository;

    public AccountJwtAuthenticationConverter(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public UsernamePasswordAuthenticationToken convert(Jwt jwt) {
        Long accountId;
        try {
            accountId = Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException ex) {
            throw new BadCredentialsException("Thông tin xác thực không hợp lệ.");
        }

        UserAccount account = userAccountRepository.findById(accountId)
                .orElseThrow(() -> new BadCredentialsException("Thông tin xác thực không hợp lệ."));

        if (account.getStatus() == AccountStatus.DISABLED) {
            throw new BadCredentialsException("Tài khoản đã bị vô hiệu hóa. Vui lòng liên hệ quản trị viên.");
        }

        if (account.getLockedUntil() != null && account.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new BadCredentialsException(
                    "Tài khoản đang tạm khóa do đăng nhập sai nhiều lần. Vui lòng thử lại sau."
            );
        }

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BadCredentialsException("Thông tin xác thực không hợp lệ.");
        }

        Number tokenVersion = jwt.getClaim("ver");
        if (tokenVersion == null || tokenVersion.intValue() != account.getTokenVersion()) {
            throw new BadCredentialsException("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
        }

        String roleClaim = jwt.getClaimAsString("role");
        if (roleClaim == null || !roleClaim.equals(account.getRole().name())) {
            throw new BadCredentialsException("Thông tin xác thực không hợp lệ.");
        }

        AccountPrincipal principal = AccountPrincipal.from(account);
        return new UsernamePasswordAuthenticationToken(principal, jwt, principal.getAuthorities());
    }
}
