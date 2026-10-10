package com.taskcenter.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

@Component
public class TelegramInitDataValidator {

    private final String botToken;
    private final long maxAgeSec;

    public TelegramInitDataValidator(
            @Value("${telegram.bot.token:}") String botToken,
            @Value("${telegram.miniapp.max-age-sec:${TELEGRAM_AUTH_MAX_AGE_SECONDS:3600}}") long maxAgeSec) {
        this.botToken = botToken;
        this.maxAgeSec = maxAgeSec;
    }

    /** To'g'ri bo'lsa parametrlar xaritasini, aks holda empty qaytaradi. */
    public Optional<Map<String, String>> validate(String initData) {
        if (botToken == null || botToken.isBlank()) {
            throw new IllegalStateException("Telegram bot token sozlanmagan! (telegram.bot.token)");
        }
        if (initData == null || initData.isBlank()) return Optional.empty();

        Map<String, String> params = new TreeMap<>();
        for (String pair : initData.split("&")) {
            int i = pair.indexOf('=');
            if (i < 0) continue;
            params.put(URLDecoder.decode(pair.substring(0, i), UTF_8),
                       URLDecoder.decode(pair.substring(i + 1), UTF_8));
        }

        String hash = params.remove("hash");
        if (hash == null) return Optional.empty();

        String dataCheck = params.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("\n"));

        byte[] secret = hmac("WebAppData".getBytes(UTF_8), botToken.getBytes(UTF_8));
        String calc = HexFormat.of().formatHex(hmac(secret, dataCheck.getBytes(UTF_8)));

        if (!MessageDigest.isEqual(calc.getBytes(UTF_8), hash.getBytes(UTF_8))) return Optional.empty();

        long authDate;
        try {
            authDate = Long.parseLong(params.getOrDefault("auth_date", "0"));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
        long now = Instant.now().getEpochSecond();
        if (authDate <= 0 || (now - authDate > maxAgeSec) || (authDate - now > 60)) {
            return Optional.empty();
        }

        return Optional.of(params);
    }

    private static byte[] hmac(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
