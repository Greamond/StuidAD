package com.example.stuid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.stuid.classes.PasswordHasher;

import org.junit.Test;

import java.util.Base64;

public class PasswordHasherTest {

    @Test
    public void test_hashPassword_returnsValidBase64String() {
        String password = "mySecurePassword123";
        String hash = PasswordHasher.hashPassword(password);

        // Проверяем, что хеш не null и может быть декодирован как Base64
        assertNotNull(hash);
        boolean decodeSuccess = true;
        try {
            Base64.getDecoder().decode(hash);
        } catch (IllegalArgumentException | NullPointerException e) {
            decodeSuccess = false;
        }
        assertTrue("Хеш должен быть корректной Base64 строкой", decodeSuccess);
    }

    @Test
    public void test_hashPassword_samePassword_returnsSameHash() {
        String password = "password123";
        String hash1 = PasswordHasher.hashPassword(password);
        String hash2 = PasswordHasher.hashPassword(password);

        assertEquals(hash1, hash2);
    }

    @Test
    public void test_hashPassword_differentPasswords_returnsDifferentHashes() {
        String password1 = "password123";
        String password2 = "password456";

        String hash1 = PasswordHasher.hashPassword(password1);
        String hash2 = PasswordHasher.hashPassword(password2);

        assertNotEquals(hash1, hash2);
    }

    @Test
    public void test_hashPassword_withNullOrEmpty_throwsIllegalArgumentException() {
        // Проверка для null
        try {
            PasswordHasher.hashPassword(null);
            // Если мы дошли до этой строки — ошибка не была выброшена
            assertFalse("Метод должен выбросить IllegalArgumentException", true);
        } catch (IllegalArgumentException e) {
            assertEquals("Password cannot be null or empty", e.getMessage());
        }

        // Проверка для пустой строки
        try {
            PasswordHasher.hashPassword("");
            // Если мы дошли до этой строки — ошибка не была выброшена
            assertFalse("Метод должен выбросить IllegalArgumentException", true);
        } catch (IllegalArgumentException e) {
            assertEquals("Password cannot be null or empty", e.getMessage());
        }
    }
}
