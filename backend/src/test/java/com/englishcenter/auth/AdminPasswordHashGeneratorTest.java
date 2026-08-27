package com.englishcenter.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AdminPasswordHashGeneratorTest {

    @Test
    @EnabledIfEnvironmentVariable(named = "ADMIN_PASSWORD_TO_HASH", matches = ".+")
    void generateBcryptHashFromLocalEnvironment() {
        String password = System.getenv("ADMIN_PASSWORD_TO_HASH");
        String hash = new BCryptPasswordEncoder(12).encode(password);

        System.out.println("Generated BCrypt hash: " + hash);
    }
}
