package com.taskcenter.util;

/**
 * Atlassian Jira va pravin-raha/lexorank4j kutubxonasiga moslashtirilgan
 * LexoRank yordamchi utiliti.
 */
public class LexoRankUtil {

    private static final String DEFAULT_RANK = "m000000000";

    /**
     * Berilgan prev va next oraliqlari orasidagi yangi leksikografik rankni hisoblaydi.
     *
     * @param prev Oldingi elementning LexoRank qiymati (yoki null - agar boshiga qo'yilayotgan bo'lsa)
     * @param next Keyingi elementning LexoRank qiymati (yoki null - agar oxiriga qo'yilayotgan bo'lsa)
     * @return Yangi o'rta LexoRank qiymati
     */
    public static String getMiddle(String prev, String next) {
        if (prev == null && next == null) {
            return DEFAULT_RANK;
        }

        boolean usesJiraFormat = (prev != null && prev.contains("|")) || (next != null && next.contains("|"));

        LexoRank p = prev != null ? LexoRank.from(prev) : null;
        LexoRank n = next != null ? LexoRank.from(next) : null;

        LexoRank mid = LexoRank.between(p, n);

        if (usesJiraFormat) {
            return mid.format();
        }

        // Agar eski raqamli yoki oddiy satr formatida bo'lsa, to'g'ridan-to'g'ri uning qiymatini qaytaradi
        return mid.getValue();
    }
}
