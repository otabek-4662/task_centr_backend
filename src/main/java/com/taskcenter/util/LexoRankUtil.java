package com.taskcenter.util;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Atlassian Jira va pravin-raha/lexorank4j kutubxonasiga moslashtirilgan
 * Professional LexoRank yordamchi utiliti.
 *
 * Imkoniyatlari:
 * - Base-36 (0-9, a-z) leksikografik o'rta qiymatni (midpoint) aniq hisoblash
 * - Jira ("0|hzzzzz:") va oddiy Base-36 ("m000000000") formatlarni qo'llab-quvvatlash
 * - Boshiga (getBefore) va oxiriga (getAfter) qo'shilganda doimiy o'lchamli (O(1) uzunlikdagi)
 *   Base-36 qadam bilan siljish (minglab vazifa qo'shilsa ham satr uzayib ketmaydi)
 * - Chuqur drag-and-drop (precision exhaustion) holatlarini aniqlash (needsRebalancing)
 * - Ustundagi barcha vazifalar uchun tekis taqsimlangan (even spacing) LexoRank ro'yxatini
 *   generatsiya qilish (generateBalancedRanks)
 */
public final class LexoRankUtil {

    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyz";
    private static final int BASE = ALPHABET.length();
    private static final BigInteger BIG_BASE = BigInteger.valueOf(BASE);
    private static final BigInteger DEFAULT_STEP = BigInteger.valueOf(36L * 36L); // "100" in Base-36 (1296)
    private static final String DEFAULT_RANK = "m000000000";

    /**
     * Rank satri ushbu uzunlikdan oshsa, ustunni avtomatik qayta muvozanatlash (rebalance) tavsiya etiladi.
     */
    public static final int MAX_SAFE_RANK_LENGTH = 48;

    /**
     * Balanslangan ranklarning standart uzunligi (36^10 ~ 3.65 * 10^15 ta pozitsiya).
     */
    private static final int BALANCED_RANK_LENGTH = 10;

    private LexoRankUtil() {
    }

    /**
     * Berilgan prev va next oraliqlari orasidagi yangi leksikografik rankni hisoblaydi.
     *
     * @param prev Oldingi elementning LexoRank qiymati (yoki null - agar boshiga qo'yilayotgan bo'lsa)
     * @param next Keyingi elementning LexoRank qiymati (yoki null - agar oxiriga qo'yilayotgan bo'lsa)
     * @return Yangi o'rta LexoRank qiymati
     */
    public static String getMiddle(String prev, String next) {
        String cleanPrev = (prev != null && !prev.isBlank()) ? prev.trim() : null;
        String cleanNext = (next != null && !next.isBlank()) ? next.trim() : null;

        if (cleanPrev == null && cleanNext == null) {
            return DEFAULT_RANK;
        }

        boolean usesJiraFormat = (cleanPrev != null && cleanPrev.contains("|"))
                || (cleanNext != null && cleanNext.contains("|"));
        if (usesJiraFormat) {
            LexoRank p = cleanPrev != null ? LexoRank.from(cleanPrev) : null;
            LexoRank n = cleanNext != null ? LexoRank.from(cleanNext) : null;
            LexoRank mid = LexoRank.between(p, n);
            return mid.format();
        }

        if (cleanPrev != null && cleanNext != null && cleanPrev.compareTo(cleanNext) >= 0) {
            throw new IllegalArgumentException("prev (" + cleanPrev + ") must be less than next (" + cleanNext + ")");
        }

        if (cleanPrev == null) {
            return getBefore(cleanNext);
        }
        if (cleanNext == null) {
            return getAfter(cleanPrev);
        }

        return computeBetweenBase36(cleanPrev, cleanNext);
    }

    /**
     * Drag-and-drop vaqtida prev >= next yoki xatolik bo'lsa ham xavfsiz o'rta qiymat qaytaradi.
     */
    public static String getMiddleSafe(String prev, String next) {
        try {
            return getMiddle(prev, next);
        } catch (IllegalArgumentException ex) {
            if (prev != null && !prev.isBlank()) {
                return prev.trim() + ALPHABET.charAt(BASE / 2);
            }
            return DEFAULT_RANK;
        }
    }

    /**
     * Berilgan rank satri qayta muvozanatlashga (rebalance) muhtojligini tekshiradi.
     */
    public static boolean needsRebalancing(String rank) {
        if (rank == null || rank.isBlank()) {
            return true;
        }
        return rank.length() > MAX_SAFE_RANK_LENGTH;
    }

    /**
     * Ikki qo'shni rank to'qnashganini yoki yangi hisoblangan rank juda uzayib ketganini tekshiradi.
     */
    public static boolean needsRebalancing(String prev, String next, String computedRank) {
        if (needsRebalancing(computedRank)) {
            return true;
        }
        String cleanPrev = (prev != null && !prev.isBlank()) ? prev.trim() : null;
        String cleanNext = (next != null && !next.isBlank()) ? next.trim() : null;
        if (cleanPrev != null && cleanNext != null && cleanPrev.compareTo(cleanNext) >= 0) {
            return true;
        }
        if (cleanPrev != null && computedRank.compareTo(cleanPrev) <= 0) {
            return true;
        }
        return cleanNext != null && computedRank.compareTo(cleanNext) >= 0;
    }

    /**
     * Ustundagi {@code count} ta vazifa uchun Base-36 fazosida tekis taqsimlangan
     * (evenly spaced) tartiblangan LexoRank qiymatlar ro'yxatini qaytaradi.
     *
     * @param count Vazifalar soni
     * @return O'sish tartibidagi yangi LexoRank satrlari
     */
    public static List<String> generateBalancedRanks(int count) {
        if (count <= 0) {
            return Collections.emptyList();
        }
        BigInteger minBound = BIG_BASE.pow(BALANCED_RANK_LENGTH - 1); // "1000000000"
        BigInteger maxBound = BIG_BASE.pow(BALANCED_RANK_LENGTH).subtract(minBound); // "z000000000"
        BigInteger totalSpan = maxBound.subtract(minBound);
        BigInteger step = totalSpan.divide(BigInteger.valueOf(count + 1L));

        if (step.compareTo(BigInteger.ONE) < 0) {
            step = BigInteger.ONE;
        }

        List<String> ranks = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            BigInteger value = minBound.add(step.multiply(BigInteger.valueOf(i)));
            ranks.add(toBase36FixedWidth(value, BALANCED_RANK_LENGTH));
        }
        return ranks;
    }

    private static String computeBetweenBase36(String prev, String next) {
        int maxLen = Math.max(prev.length(), next.length());
        int minLen = Math.min(prev.length(), next.length());
        String p = padRight(prev, maxLen);
        String n = padRight(next, maxLen);

        int[] pDigits = new int[maxLen];
        int[] nDigits = new int[maxLen];
        for (int i = 0; i < maxLen; i++) {
            pDigits[i] = digitOf(p.charAt(i));
            nDigits[i] = digitOf(n.charAt(i));
        }

        int[] sum = new int[maxLen + 1];
        int addCarry = 0;
        for (int i = maxLen - 1; i >= 0; i--) {
            int s = pDigits[i] + nDigits[i] + addCarry;
            sum[i + 1] = s % BASE;
            addCarry = s / BASE;
        }
        sum[0] = addCarry;

        StringBuilder mid = new StringBuilder(maxLen + 2);
        int divRem = 0;
        for (int i = 0; i <= maxLen; i++) {
            int s = sum[i] + divRem * BASE;
            int midVal = s / 2;
            divRem = s % 2;
            if (i == 0 && midVal == 0) {
                continue;
            }
            mid.append(ALPHABET.charAt(midVal));
        }

        if (divRem > 0) {
            mid.append(ALPHABET.charAt((divRem * BASE) / 2));
        }

        String result = mid.toString();
        while (result.length() > minLen && result.length() > 1 && result.endsWith("0")) {
            result = result.substring(0, result.length() - 1);
        }

        if (result.compareTo(prev) <= 0 || result.compareTo(next) >= 0) {
            result = prev + ALPHABET.charAt(BASE / 2);
        }

        return result;
    }

    private static String getBefore(String current) {
        if (current.equals("0") || current.chars().allMatch(ch -> ch == '0')) {
            throw new IllegalArgumentException("Cannot get rank before 0");
        }
        int width = Math.max(BALANCED_RANK_LENGTH, current.length());
        String padded = padRight(current, width);
        BigInteger curVal = fromBase36(padded);
        if (curVal.compareTo(DEFAULT_STEP) > 0) {
            BigInteger prevVal = curVal.subtract(DEFAULT_STEP);
            String candidate = toBase36FixedWidth(prevVal, width);
            if (candidate.compareTo(current) < 0) {
                return candidate;
            }
        }
        return computeBetweenBase36("0", current);
    }

    private static String getAfter(String current) {
        int width = Math.max(BALANCED_RANK_LENGTH, current.length());
        String padded = padRight(current, width);
        BigInteger curVal = fromBase36(padded);
        BigInteger maxVal = BIG_BASE.pow(width).subtract(BigInteger.ONE);
        if (curVal.add(DEFAULT_STEP).compareTo(maxVal) < 0) {
            BigInteger nextVal = curVal.add(DEFAULT_STEP);
            String candidate = toBase36FixedWidth(nextVal, width);
            if (candidate.compareTo(current) > 0) {
                return candidate;
            }
        }
        return current + ALPHABET.charAt(BASE / 2);
    }

    private static BigInteger fromBase36(String s) {
        BigInteger val = BigInteger.ZERO;
        for (int i = 0; i < s.length(); i++) {
            val = val.multiply(BIG_BASE).add(BigInteger.valueOf(digitOf(s.charAt(i))));
        }
        return val;
    }

    private static int digitOf(char c) {
        char lower = Character.toLowerCase(c);
        int idx = ALPHABET.indexOf(lower);
        return Math.max(idx, 0);
    }

    private static String toBase36FixedWidth(BigInteger value, int width) {
        String raw = value.toString(BASE).toLowerCase();
        if (raw.length() >= width) {
            return raw;
        }
        return "0".repeat(width - raw.length()) + raw;
    }

    private static String padRight(String s, int n) {
        if (s.length() >= n) {
            return s;
        }
        StringBuilder sb = new StringBuilder(n);
        sb.append(s);
        while (sb.length() < n) {
            sb.append('0');
        }
        return sb.toString();
    }
}
