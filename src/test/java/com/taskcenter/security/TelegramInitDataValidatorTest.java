package com.taskcenter.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

class TelegramInitDataValidatorTest {

    private static final String BOT_TOKEN = "123456789:ABCdefGhIJKlmNoPQRsTUVwxyZ";
    private static final long MAX_AGE_SEC = 3600; // 1 hour

    private TelegramInitDataValidator validator;

    @BeforeEach
    void setUp() {
        validator = new TelegramInitDataValidator(BOT_TOKEN, MAX_AGE_SEC);
    }

    private String generateInitData(Map<String, String> params, String botToken) {
        Map<String, String> sortedParams = new TreeMap<>(params);
        String dataCheck = sortedParams.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("\n"));

        byte[] secret = hmac("WebAppData".getBytes(UTF_8), botToken.getBytes(UTF_8));
        String hash = HexFormat.of().formatHex(hmac(secret, dataCheck.getBytes(UTF_8)));

        return sortedParams.entrySet().stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), UTF_8))
                .collect(Collectors.joining("&")) + "&hash=" + hash;
    }

    private static byte[] hmac(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("valid initData muvaffaqiyatli tekshirilib parametrlar qaytadi")
    void validate_validInitData_returnsParameters() {
        Map<String, String> rawParams = new LinkedHashMap<>();
        rawParams.put("auth_date", String.valueOf(Instant.now().getEpochSecond() - 60)); // 1 min old
        rawParams.put("query_id", "AAHdF6IQAAAAAN0XohDhrOrc");
        rawParams.put("user", "{\"id\":12345678,\"first_name\":\"Bekmurod\",\"username\":\"bekmurod\"}");

        String initData = generateInitData(rawParams, BOT_TOKEN);

        Optional<Map<String, String>> result = validator.validate(initData);

        assertThat(result).isPresent();
        Map<String, String> map = result.get();
        assertThat(map.get("query_id")).isEqualTo("AAHdF6IQAAAAAN0XohDhrOrc");
        assertThat(map.get("user")).contains("Bekmurod");
        assertThat(map.get("auth_date")).isEqualTo(rawParams.get("auth_date"));
    }

    @Test
    @DisplayName("tampered hash bo'lsa Optional.empty qaytadi")
    void validate_tamperedHash_returnsEmpty() {
        Map<String, String> rawParams = new LinkedHashMap<>();
        rawParams.put("auth_date", String.valueOf(Instant.now().getEpochSecond() - 60));
        rawParams.put("query_id", "AAHdF6IQAAAAAN0XohDhrOrc");
        rawParams.put("user", "{\"id\":12345678,\"first_name\":\"Bekmurod\"}");

        String validInitData = generateInitData(rawParams, BOT_TOKEN);
        // Tamper the hash by replacing last characters
        String tamperedInitData = validInitData.substring(0, validInitData.length() - 4) + "0000";

        Optional<Map<String, String>> result = validator.validate(tamperedInitData);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("expired auth_date (muddati o'tgan) bo'lsa Optional.empty qaytadi")
    void validate_expiredAuthDate_returnsEmpty() {
        Map<String, String> rawParams = new LinkedHashMap<>();
        // auth_date is 2 hours ago (MAX_AGE_SEC is 1 hour)
        rawParams.put("auth_date", String.valueOf(Instant.now().getEpochSecond() - 7200));
        rawParams.put("query_id", "AAHdF6IQAAAAAN0XohDhrOrc");
        rawParams.put("user", "{\"id\":12345678,\"first_name\":\"Bekmurod\"}");

        String expiredInitData = generateInitData(rawParams, BOT_TOKEN);

        Optional<Map<String, String>> result = validator.validate(expiredInitData);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("hash yo'q bo'lsa Optional.empty qaytadi")
    void validate_missingHash_returnsEmpty() {
        String initDataWithoutHash = "query_id=AAHdF6IQAAAAAN0XohDhrOrc&auth_date=" + (Instant.now().getEpochSecond() - 60);

        Optional<Map<String, String>> result = validator.validate(initDataWithoutHash);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("kelajak auth_date (60s dan ko'p) bo'lsa Optional.empty qaytadi")
    void validate_futureAuthDate_returnsEmpty() {
        Map<String, String> rawParams = new LinkedHashMap<>();
        // auth_date is 5 minutes in the future
        rawParams.put("auth_date", String.valueOf(Instant.now().getEpochSecond() + 300));
        rawParams.put("query_id", "AAHdF6IQAAAAAN0XohDhrOrc");
        rawParams.put("user", "{\"id\":12345678,\"first_name\":\"Bekmurod\"}");

        String futureInitData = generateInitData(rawParams, BOT_TOKEN);

        Optional<Map<String, String>> result = validator.validate(futureInitData);

        assertThat(result).isEmpty();
    }
}
