package com.taskcenter.util;

public class LexoRankUtil {

    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyz";
    private static final int BASE = ALPHABET.length();
    private static final String DEFAULT_RANK = "0000000000";

    public static String getMiddle(String prev, String next) {
        if (prev == null && next == null) {
            return DEFAULT_RANK; // Birinchi task
        }
        if (prev == null) {
            return getBefore(next);
        }
        if (next == null) {
            return getAfter(prev);
        }
        if (prev.compareTo(next) >= 0) {
            throw new IllegalArgumentException("prev (" + prev + ") must be less than next (" + next + ")");
        }

        // Make lengths equal
        int maxLen = Math.max(prev.length(), next.length());
        prev = padRight(prev, maxLen);
        next = padRight(next, maxLen);

        String mid = calculateMiddle(prev, next);
        
        // Agar o'rtacha qiymat prev ga teng bo'lib qolsa (yaxlitlash tufayli), bitta belgi qo'shamiz
        if (mid.equals(prev)) {
            mid = mid + ALPHABET.charAt(BASE / 2);
        }

        return mid;
    }

    private static String getAfter(String current) {
        // Oxirgi belgini oshiramiz, agar 'z' bo'lsa, 'm' qo'shamiz
        int lastIndex = current.length() - 1;
        char lastChar = current.charAt(lastIndex);
        if (lastChar < 'z') {
            int charIndex = ALPHABET.indexOf(lastChar);
            return current.substring(0, lastIndex) + ALPHABET.charAt(charIndex + 1);
        } else {
            return current + ALPHABET.charAt(BASE / 2); // 'm'
        }
    }

    private static String getBefore(String current) {
        // Eng birinchi belgini kamaytiramiz, yoki agar '0' bo'lsa oldiga '0' qo'shib orqasiga qo'shamiz
        // Bu murakkab bo'lmasligi uchun, shunchaki '0' dan keyingi harfni yarmigacha qisqartiramiz
        return calculateMiddle(padRight("", current.length()), current);
    }

    private static String padRight(String s, int n) {
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < n) {
            sb.append(ALPHABET.charAt(0));
        }
        return sb.toString();
    }

    private static String calculateMiddle(String prev, String next) {
        StringBuilder result = new StringBuilder();
        int carry = 0;
        
        for (int i = 0; i < prev.length(); i++) {
            int pIndex = ALPHABET.indexOf(prev.charAt(i));
            int nIndex = ALPHABET.indexOf(next.charAt(i));
            
            if (pIndex == -1) pIndex = 0;
            if (nIndex == -1) nIndex = 0;

            int sum = pIndex + nIndex + carry * BASE;
            int midIndex = sum / 2;
            carry = sum % 2;
            
            result.append(ALPHABET.charAt(midIndex));
        }
        
        if (carry > 0) {
            result.append(ALPHABET.charAt((carry * BASE) / 2));
        }
        
        return result.toString();
    }
}
