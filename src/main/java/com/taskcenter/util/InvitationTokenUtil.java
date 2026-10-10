package com.taskcenter.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

public final class InvitationTokenUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private InvitationTokenUtil() {
    }

    /**
     * Kamida 32 baytli tasodifiy URL-safe token generatsiya qiladi (43 belgi).
     */
    public static String generateToken() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * Xom tokenni SHA-256 orqali 64 ta hex belgidan iborat hash ko'rinishiga aylantiradi.
     */
    public static String hashToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("Token bo'sh bo'lishi mumkin emas");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.trim().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algoritmi topilmadi", e);
        }
    }

    /**
     * Email manzilini xavfsiz niqoblaydi (masalan: alice@gmail.com -> a***@gmail.com).
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int atIdx = email.indexOf('@');
        String local = email.substring(0, atIdx);
        String domain = email.substring(atIdx);
        if (local.isEmpty()) {
            return "***" + domain;
        }
        return local.charAt(0) + "***" + domain;
    }
}
