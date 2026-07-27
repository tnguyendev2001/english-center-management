package com.englishcenter.auth;

import com.englishcenter.common.config.JwtProperties;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {
    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    public JwtTokenService(JwtEncoder jwtEncoder, JwtProperties jwtProperties) {
        this.jwtEncoder = jwtEncoder;
        this.jwtProperties = jwtProperties;
    }

    public IssuedToken issueToken(UserAccount account) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(jwtProperties.getExpirationMinutes() * 60);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .id(UUID.randomUUID().toString())
                .issuer(jwtProperties.getIssuer())
                .audience(java.util.List.of(jwtProperties.getAudience()))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(String.valueOf(account.getId()))
                .claim("username", account.getUsername())
                .claim("role", account.getRole().name())
                .claim("ver", account.getTokenVersion())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        Jwt jwt = jwtEncoder.encode(JwtEncoderParameters.from(header, claims));
        long expiresIn = jwtProperties.getExpirationMinutes() * 60;
        return new IssuedToken(jwt.getTokenValue(), expiresIn);
    }

    public record IssuedToken(String accessToken, long expiresInSeconds) {
    }
}
