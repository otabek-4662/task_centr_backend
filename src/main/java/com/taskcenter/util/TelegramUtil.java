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
}
