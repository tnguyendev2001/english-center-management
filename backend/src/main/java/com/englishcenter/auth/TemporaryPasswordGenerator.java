package com.englishcenter.auth;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class TemporaryPasswordGenerator {
    private static final String LETTERS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String ALL = LETTERS + DIGITS;
    private static final SecureRandom RANDOM = new SecureRandom();

    public String generate() {
        char[] password = new char[12];
        password[0] = LETTERS.charAt(RANDOM.nextInt(LETTERS.length()));
        password[1] = DIGITS.charAt(RANDOM.nextInt(DIGITS.length()));
        for (int i = 2; i < password.length; i++) {
            password[i] = ALL.charAt(RANDOM.nextInt(ALL.length()));
        }
        for (int i = password.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char tmp = password[i];
            password[i] = password[j];
            password[j] = tmp;
        }
        return new String(password);
    }
}
