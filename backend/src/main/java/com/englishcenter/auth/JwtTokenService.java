package com.englishcenter.auth;

import com.englishcenter.common.config.AppSecurityProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private static final List<String> ADMIN_ROLES = List.of("ADMIN");

    private final JwtEncoder jwtEncoder;
    private final AppSecurityProperties securityProperties;

    public JwtTokenService(JwtEncoder jwtEncoder, AppSecurityProperties securityProperties) {
        this.jwtEncoder = jwtEncoder;
        this.securityProperties = securityProperties;
    }

    public String generateAccessToken(String username) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(getExpirationSeconds());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(securityProperties.getJwt().getIssuer())
                .subject(username)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("roles", ADMIN_ROLES)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256)
                .type("JWT")
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long getExpirationSeconds() {
        return Duration.ofMinutes(securityProperties.getJwt().getExpirationMinutes()).toSeconds();
    }
}
