package com.taskcenter.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LexoRank & LexoRankUtil — Jira & lexorank4j Testlari")
class LexoRankTest {

    @Test
    @DisplayName("Standart Jira formatidagi LexoRank orasiga joylashtirish (between)")
    void testBetweenJiraFormat() {
        LexoRank r1 = LexoRank.from("0|hzzzzz:");
        LexoRank r2 = LexoRank.from("0|izzzzz:");

        LexoRank mid = LexoRank.between(r1, r2);

        assertNotNull(mid);
        assertTrue(r1.compareTo(mid) < 0, "r1 mid dan kichik bo'lishi kerak");
        assertTrue(mid.compareTo(r2) < 0, "mid r2 dan kichik bo'lishi kerak");
    }

    @Test
    @DisplayName("Oddiy raqamli formatdagi satrlar orasiga joylashtirish")
    void testBetweenNumericStrings() {
        String mid = LexoRankUtil.getMiddle("0000001000", "0000002000");

        assertNotNull(mid);
        assertTrue("0000001000".compareTo(mid) < 0);
        assertTrue(mid.compareTo("0000002000") < 0);
    }

    @Test
    @DisplayName("Eng yuqoriga (Top/Before) qo'yganda eng kichik rank olinadi")
    void testBeforeRank() {
        LexoRank first = LexoRank.from("0|100000:");
        LexoRank beforeFirst = LexoRank.before(first);

        assertTrue(beforeFirst.compareTo(first) < 0);
    }

    @Test
    @DisplayName("Eng oxiriga (Bottom/After) qo'yganda eng katta rank olinadi")
    void testAfterRank() {
        LexoRank last = LexoRank.from("0|hzzzzz:");
        LexoRank afterLast = LexoRank.after(last);

        assertTrue(afterLast.compareTo(last) > 0);
    }

    @Test
    @DisplayName("Precision kengayishi (Adjacent characters collision avoidance): 'a' va 'b' orasiga 'am' qo'yiladi")
    void testPrecisionExpansion() {
        LexoRank r1 = LexoRank.from("0|a:");
        LexoRank r2 = LexoRank.from("0|b:");

        LexoRank mid = LexoRank.between(r1, r2);

        assertTrue(r1.compareTo(mid) < 0);
        assertTrue(mid.compareTo(r2) < 0);
        assertTrue(mid.getValue().startsWith("a"));
    }

    @Test
    @DisplayName("Self-healing: Agar noto'g'ri prev >= next kelsa xato bermaydi, balki prev'dan keyingi rankni oladi")
    void testSelfHealingOnInvalidRange() {
        LexoRank r1 = LexoRank.from("0|izzzzz:");
        LexoRank r2 = LexoRank.from("0|hzzzzz:"); // r2 < r1 (noto'g'ri oraliq)

        LexoRank mid = LexoRank.between(r1, r2);

        assertNotNull(mid);
        assertTrue(r1.compareTo(mid) < 0, "Self-healing natijasida r1 dan katta yangi rank olinishi kerak");
    }
}
