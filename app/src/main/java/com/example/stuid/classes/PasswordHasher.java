package com.example.stuid.classes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordHasher {
    public static String hashPassword(String password) {
        try {
            if (password == null || password.isEmpty()) {
                throw new IllegalArgumentException("Password cannot be null or empty");
            }

            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] hashedPassword = digest.digest(password.getBytes(StandardCharsets.UTF_8));

            for (int i = 0; i < 1000; i++) {
                digest.reset();
                hashedPassword = digest.digest(hashedPassword);
            }

            return Base64.getEncoder().encodeToString(hashedPassword);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }
}
