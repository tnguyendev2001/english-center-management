package com.englishcenter.auth;

import com.englishcenter.auth.dto.LoginRequest;
import com.englishcenter.auth.dto.LoginResponse;
import com.englishcenter.common.config.AppSecurityProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid username or password";

    private final AppSecurityProperties securityProperties;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthService(
            AppSecurityProperties securityProperties,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService
    ) {
        this.securityProperties = securityProperties;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    public LoginResponse login(LoginRequest request) {
        String configuredUsername = securityProperties.getAdmin().getUsername();
        boolean usernameMatches = MessageDigest.isEqual(
                request.getUsername().getBytes(StandardCharsets.UTF_8),
                configuredUsername.getBytes(StandardCharsets.UTF_8)
        );
        boolean passwordMatches = passwordEncoder.matches(
                request.getPassword(),
                securityProperties.getAdmin().getPasswordHash()
        );

        if (!usernameMatches || !passwordMatches) {
            throw new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE);
        }

        return new LoginResponse(
                jwtTokenService.generateAccessToken(configuredUsername),
                "Bearer",
                jwtTokenService.getExpirationSeconds()
        );
    }
}
