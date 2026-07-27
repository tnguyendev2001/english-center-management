package com.englishcenter.auth.security;

import com.englishcenter.auth.AccountRole;
import com.englishcenter.auth.AccountStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record AccountPrincipal(
        Long id,
        String username,
        AccountRole role,
        AccountStatus status,
        Long studentId,
        Long teacherId,
        boolean mustChangePassword,
        int tokenVersion
) implements UserDetails {
    public static AccountPrincipal from(com.englishcenter.auth.UserAccount account) {
        return new AccountPrincipal(
                account.getId(),
                account.getUsername(),
                account.getRole(),
                account.getStatus(),
                account.getStudentId(),
                account.getTeacherId(),
                account.isMustChangePassword(),
                account.getTokenVersion()
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != AccountStatus.LOCKED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == AccountStatus.ACTIVE;
    }
}
