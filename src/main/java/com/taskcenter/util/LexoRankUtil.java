package com.taskcenter.util;

public class LexoRankUtil {

    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyz";
    private static final int BASE = ALPHABET.length();
    private static final String DEFAULT_RANK = "m000000000";

    public static String getMiddle(String prev, String next) {
        if (prev == null && next == null) {
            return DEFAULT_RANK;
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

        int maxLen = Math.max(prev.length(), next.length());
        String p = padRight(prev, maxLen);
        String n = padRight(next, maxLen);

        int[] pDigits = new int[maxLen];
        int[] nDigits = new int[maxLen];
        for (int i = 0; i < maxLen; i++) {
            pDigits[i] = ALPHABET.indexOf(p.charAt(i));
            nDigits[i] = ALPHABET.indexOf(n.charAt(i));
        }

        int[] sum = new int[maxLen + 1];
        int addCarry = 0;
        for (int i = maxLen - 1; i >= 0; i--) {
            int s = pDigits[i] + nDigits[i] + addCarry;
            sum[i + 1] = s % BASE;
            addCarry = s / BASE;
        }
        sum[0] = addCarry;

        StringBuilder mid = new StringBuilder();
        int divRem = 0;
        for (int i = 0; i <= maxLen; i++) {
            int s = sum[i] + divRem * BASE;
            int midVal = s / 2;
            divRem = s % 2;
            if (i == 0 && midVal == 0) continue;
            mid.append(ALPHABET.charAt(midVal));
        }
        
        if (divRem > 0) {
            mid.append(ALPHABET.charAt((divRem * BASE) / 2));
        }
        
        String result = mid.toString();
        // Remove trailing zeros for shorter strings
        while (result.length() > 1 && result.endsWith("0")) {
            result = result.substring(0, result.length() - 1);
        }
        
        // Failsafe in case result is not strictly between
        if (result.compareTo(prev) <= 0 || result.compareTo(next) >= 0) {
            result = prev + ALPHABET.charAt(BASE / 2);
        }
        
        return result;
    }

    private static String getBefore(String current) {
        if (current.equals("0")) {
            throw new IllegalArgumentException("Cannot get rank before 0");
        }
        return getMiddle("0", current);
    }

    private static String getAfter(String current) {
        return getMiddle(current, "z");
    }

    private static String padRight(String s, int n) {
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < n) {
            sb.append('0');
        }
        return sb.toString();
    }
}
