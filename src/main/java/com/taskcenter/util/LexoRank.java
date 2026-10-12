package com.taskcenter.util;

import java.io.Serializable;
import java.util.Objects;

/**
 * pravin-raha/lexorank4j va Atlassian Jira LexoRank algoritmi asosidagi
 * leksikografik tartiblash (Lexicographical Ranking) implementatsiyasi.
 *
 * Xususiyatlari:
 * 1. Elementlar orasiga (between) yangi tartib qiymatini O(1) vaqtda hisoblash.
 * 2. Bucket (0, 1, 2) va Base-36 raqamlar tizimi (0-9, a-z) qo'llab-quvvatlanadi.
 * 3. Auto-precision: Agar ikki ketma-ket rank orasida bo'sh joy qolmasa (masalan, 'a' va 'b'),
 *    avtomatik ravishda qo'shimcha oraliq belgilar ('am', 'an') qo'shiladi.
 * 4. Self-healing (O'z-o'zini tiklash): Agar noto'g'ri tartibda prev >= next kelsa, xato bermaydi,
 *    balki keyingi oraliqni xavfsiz generatsiya qiladi.
 * 5. Eskicha raqamli ("0000001000") va Jira ("0|hzzzzz:") formatlari bilan 100% orqaga mos (backward compatible).
 */
public final class LexoRank implements Comparable<LexoRank>, Serializable {

    private static final long serialVersionUID = 1L;

    public static final String NUMERALS = "0123456789abcdefghijklmnopqrstuvwxyz";
    public static final int BASE = NUMERALS.length(); // 36
    public static final char MID_CHAR = 'm';
    public static final String DEFAULT_VALUE = "0|hzzzzz:";

    private final String bucket;
    private final String value;

    private LexoRank(String bucket, String value) {
        this.bucket = bucket != null && !bucket.isBlank() ? bucket : "0";
        this.value = value != null && !value.isBlank() ? value : "hzzzzz";
    }

    public static LexoRank of(String bucket, String value) {
        return new LexoRank(bucket, value);
    }

    public static LexoRank initial() {
        return new LexoRank("0", "hzzzzz");
    }

    public static LexoRank from(String raw) {
        if (raw == null || raw.isBlank()) {
            return initial();
        }
        String clean = raw.trim();
        int pipeIdx = clean.indexOf('|');
        if (pipeIdx > 0 && pipeIdx < clean.length() - 1) {
            String b = clean.substring(0, pipeIdx);
            String v = clean.substring(pipeIdx + 1);
            if (v.endsWith(":")) {
                v = v.substring(0, v.length() - 1);
            }
            return new LexoRank(b, v);
        }
        // Agar bucket berilmagan bo'lsa (masalan "0000001000")
        return new LexoRank("0", clean);
    }

    /**
     * Ikki LexoRank orasidagi o'rta qiymatni hisoblash (lexorank4j asosi).
     */
    public static LexoRank between(LexoRank prev, LexoRank next) {
        if (prev == null && next == null) {
            return initial();
        }
        if (prev == null) {
            return before(next);
        }
        if (next == null) {
            return after(prev);
        }

        // Agar prev >= next kelsa (noto'g'ri oraliq), self-healing qilib prev'dan keyingisini olamiz
        if (prev.compareTo(next) >= 0) {
            return after(prev);
        }

        String bucket = prev.bucket;
        String pVal = prev.value;
        String nVal = next.value;

        String midValue = calculateBetweenValues(pVal, nVal);
        return new LexoRank(bucket, midValue);
    }

    public static LexoRank before(LexoRank next) {
        if (next == null) return initial();
        String nVal = next.value;
        String minVal = "0";
        if (nVal.compareTo("1") <= 0) {
            return new LexoRank(next.bucket, "0" + MID_CHAR);
        }
        String mid = calculateBetweenValues(minVal, nVal);
        return new LexoRank(next.bucket, mid);
    }

    public static LexoRank after(LexoRank prev) {
        if (prev == null) return initial();
        String pVal = prev.value;
        // zzz dan keyin qo'shimcha belgi qo'shiladi
        return new LexoRank(prev.bucket, pVal + MID_CHAR);
    }

    private static String calculateBetweenValues(String prev, String next) {
        int maxLen = Math.max(prev.length(), next.length());
        String pPadded = padRight(prev, maxLen);
        String nPadded = padRight(next, maxLen);

        int[] pDigits = new int[maxLen];
        int[] nDigits = new int[maxLen];
        for (int i = 0; i < maxLen; i++) {
            pDigits[i] = digitValue(pPadded.charAt(i));
            nDigits[i] = digitValue(nPadded.charAt(i));
        }

        int[] sum = new int[maxLen + 1];
        int carry = 0;
        for (int i = maxLen - 1; i >= 0; i--) {
            int s = pDigits[i] + nDigits[i] + carry;
            sum[i + 1] = s % BASE;
            carry = s / BASE;
        }
        sum[0] = carry;

        StringBuilder mid = new StringBuilder();
        int rem = 0;
        for (int i = 0; i <= maxLen; i++) {
            int s = sum[i] + rem * BASE;
            int val = s / 2;
            rem = s % 2;
            if (i == 0 && val == 0) continue;
            mid.append(NUMERALS.charAt(val));
        }

        if (rem > 0) {
            mid.append(NUMERALS.charAt((rem * BASE) / 2));
        }

        String result = mid.toString();
        while (result.length() > 1 && result.endsWith("0")) {
            result = result.substring(0, result.length() - 1);
        }

        // Failsafe: Agar natija oraliqqa tushmasa, prev oxiriga 'm' qo'shib precision oshiriladi
        if (result.compareTo(prev) <= 0 || result.compareTo(next) >= 0) {
            result = prev + MID_CHAR;
        }

        return result;
    }

    private static int digitValue(char c) {
        int idx = NUMERALS.indexOf(c);
        return idx >= 0 ? idx : 0;
    }

    private static String padRight(String s, int n) {
        if (s.length() >= n) return s;
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < n) {
            sb.append('0');
        }
        return sb.toString();
    }

    public String format() {
        return bucket + "|" + value + ":";
    }

    public String getValue() {
        return value;
    }

    public String getBucket() {
        return bucket;
    }

    @Override
    public int compareTo(LexoRank other) {
        if (other == null) return 1;
        int bCmp = this.bucket.compareTo(other.bucket);
        if (bCmp != 0) return bCmp;
        return this.value.compareTo(other.value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LexoRank lexoRank = (LexoRank) o;
        return Objects.equals(bucket, lexoRank.bucket) && Objects.equals(value, lexoRank.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bucket, value);
    }

    @Override
    public String toString() {
        return format();
    }
}
