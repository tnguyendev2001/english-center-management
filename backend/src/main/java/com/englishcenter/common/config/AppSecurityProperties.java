package com.englishcenter.common.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.security")
public class AppSecurityProperties {

    @Valid
    private final Admin admin = new Admin();

    @Valid
    private final Jwt jwt = new Jwt();

    public Admin getAdmin() {
        return admin;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public static class Admin {

        @NotBlank(message = "ADMIN_USERNAME is required")
        private String username;

        @NotBlank(message = "ADMIN_PASSWORD_HASH is required")
        private String passwordHash;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPasswordHash() {
            return passwordHash;
        }

        public void setPasswordHash(String passwordHash) {
            this.passwordHash = passwordHash;
        }
    }

    public static class Jwt {

        @NotBlank(message = "JWT_SECRET is required")
        private String secret;

        @NotBlank(message = "JWT_ISSUER is required")
        private String issuer;

        @Positive(message = "JWT_EXPIRATION_MINUTES must be greater than zero")
        private long expirationMinutes = 480;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public long getExpirationMinutes() {
            return expirationMinutes;
        }

        public void setExpirationMinutes(long expirationMinutes) {
            this.expirationMinutes = expirationMinutes;
        }
    }
}
