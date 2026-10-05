package com.taskcenter.util;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TelegramUtil {

    private static final Pattern MENTION_PATTERN = Pattern.compile("(?<![a-zA-Z0-9_.])@([a-zA-Z0-9_.-]+)");

    private TelegramUtil() {}

    public static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;");
    }

    /**
     * Berilgan vaqt sokin soatlar orasida ekanligini tekshiradi.
     * Tun bo'ylab o'tadigan oraliqlar ham qo'llab-quvvatlanadi (masalan, 22:00–08:00).
     */
    public static boolean isInQuietHours(LocalTime now, LocalTime start, LocalTime end) {
        if (start == null || end == null) return false;
        if (start.isBefore(end)) {
            // Kunduzi: masalan, 08:00–22:00
            return !now.isBefore(start) && now.isBefore(end);
        } else {
            // Tun bo'ylab: masalan, 22:00–08:00
            return !now.isBefore(start) || now.isBefore(end);
        }
    }

    /**
     * Matndagi @username larni ajratib oladi.
     * @ oldida harf/raqam/_/. bo'lmasligi kerak (masalan email dagi @gmail.com olinmaydi).
     * Oxiridagi . va - belgilar kesiladi (masalan "@ali." -> "ali").
     */
    public static Set<String> extractMentionedUsernames(String content) {
        Set<String> usernames = new LinkedHashSet<>();
        if (content == null || content.isBlank()) {
            return usernames;
        }
        Matcher matcher = MENTION_PATTERN.matcher(content);
        while (matcher.find()) {
            String raw = matcher.group(1);
            String username = raw.replaceAll("[.-]+$", "");
            if (!username.isBlank()) {
                usernames.add(username);
            }
        }
        return usernames;
    }

    /**
     * Izoh matnini ko'pi bilan maxLength (masalan 100) belgigacha kesadi va escapeHtml qiladi.
     * Agar kesish so'z o'rtasida bo'lsa "..." qo'shiladi.
     * Avval 100 belgigacha kesiladi, so'ngra escapeHtml qilinadi.
     */
    public static String truncateComment(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return escapeHtml(text);
        }
        String sub = text.substring(0, maxLength);
        boolean cutInsideWord = !Character.isWhitespace(text.charAt(maxLength - 1))
                && !Character.isWhitespace(text.charAt(maxLength));
        if (cutInsideWord) {
            sub = sub + "...";
        }
        return escapeHtml(sub);
    }

    /**
     * TASK_VIEW inline tugmasini yaratadi.
     */
    public static InlineKeyboardMarkup createTaskViewKeyboard(String taskId, String title) {
        if (taskId == null) {
            return null;
        }
        InlineKeyboardButton btn = new InlineKeyboardButton();
        String btnTitle = title != null ? title : "";
        if (btnTitle.length() > 30) {
            btnTitle = btnTitle.substring(0, 27) + "...";
        }
        btn.setText("📋 " + (!btnTitle.isBlank() ? btnTitle : "Vazifani ko'rish"));
        btn.setCallbackData("TASK_VIEW_" + taskId);
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        keyboard.setKeyboard(List.of(List.of(btn)));
        return keyboard;
    }

    private static final com.fasterxml.jackson.databind.ObjectMapper OBJECT_MAPPER = new com.fasterxml.jackson.databind.ObjectMapper();

    public record TelegramUserData(Long id, String firstName, String lastName, String username) {
        public String getDisplayName() {
            if (firstName != null && !firstName.isBlank()) {
                return (lastName != null && !lastName.isBlank()) ? firstName + " " + lastName : firstName;
            }
            if (username != null && !username.isBlank()) {
                return username;
            }
            return "Telegram User " + id;
        }
    }

    /**
     * Telegram WebApp tomonidan yuborilgan initData query-string'ni tekshiradi (HMAC-SHA-256).
     * Agar ma'lumotlar haqiqiy bo'lsa, TelegramUserData obyektini qaytaradi, aks holda null.
     */
    public static TelegramUserData validateTelegramWebAppData(String initData, String botToken) {
        if (initData == null || initData.isBlank() || botToken == null || botToken.isBlank()) {
            return null;
        }
        try {
            java.util.Map<String, String> params = new java.util.LinkedHashMap<>();
            String[] pairs = initData.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                if (idx > 0) {
                    String key = pair.substring(0, idx);
                    String val = java.net.URLDecoder.decode(pair.substring(idx + 1), java.nio.charset.StandardCharsets.UTF_8);
                    params.put(key, val);
                }
            }
            String receivedHash = params.remove("hash");
            if (receivedHash == null || receivedHash.isBlank()) {
                return null;
            }

            java.util.List<String> sortedKeys = new java.util.ArrayList<>(params.keySet());
            java.util.Collections.sort(sortedKeys);

            StringBuilder checkString = new StringBuilder();
            for (String key : sortedKeys) {
                if (checkString.length() > 0) {
                    checkString.append("\n");
                }
                checkString.append(key).append("=").append(params.get(key));
            }

            javax.crypto.Mac hmacSha256 = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKeySpec = new javax.crypto.spec.SecretKeySpec(
                    "WebAppData".getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
            hmacSha256.init(secretKeySpec);
            byte[] secretKey = hmacSha256.doFinal(botToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            javax.crypto.Mac dataHmac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec dataKeySpec = new javax.crypto.spec.SecretKeySpec(secretKey, "HmacSHA256");
            dataHmac.init(dataKeySpec);
            byte[] calculatedHashBytes = dataHmac.doFinal(checkString.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));

            StringBuilder hexSb = new StringBuilder();
            for (byte b : calculatedHashBytes) {
                hexSb.append(String.format("%02x", b));
            }
            String calculatedHash = hexSb.toString();

            if (!java.security.MessageDigest.isEqual(
                    calculatedHash.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    receivedHash.toLowerCase().getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
                return null;
            }

            String userJson = params.get("user");
            if (userJson == null || userJson.isBlank()) {
                return null;
            }

            com.fasterxml.jackson.databind.JsonNode userNode = OBJECT_MAPPER.readTree(userJson);
            Long id = userNode.has("id") ? userNode.get("id").asLong() : null;
            String firstName = userNode.has("first_name") ? userNode.get("first_name").asText() : "";
            String lastName = userNode.has("last_name") ? userNode.get("last_name").asText() : "";
            String username = userNode.has("username") ? userNode.get("username").asText() : null;

            if (id == null) {
                return null;
            }

            return new TelegramUserData(id, firstName, lastName, username);
        } catch (Exception e) {
            return null;
        }
    }
}

