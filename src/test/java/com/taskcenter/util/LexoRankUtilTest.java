package com.taskcenter.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LexoRankUtilTest {

    @Test
    void testDefaultRank() {
        String rank = LexoRankUtil.getMiddle(null, null);
        assertEquals("m000000000", rank);
    }

    @Test
    void testGetBefore() {
        String next = "m000000000";
        String before = LexoRankUtil.getMiddle(null, next);
        assertNotNull(before);
        assertTrue(before.compareTo(next) < 0, "Before must be less than next");

        String beforeBefore = LexoRankUtil.getMiddle(null, before);
        assertTrue(beforeBefore.compareTo(before) < 0, "beforeBefore must be less than before");
    }

    @Test
    void testGetAfter() {
        String prev = "m000000000";
        String after = LexoRankUtil.getMiddle(prev, null);
        assertNotNull(after);
        assertTrue(after.compareTo(prev) > 0, "After must be greater than prev");

        String afterAfter = LexoRankUtil.getMiddle(after, null);
        assertTrue(afterAfter.compareTo(after) > 0, "afterAfter must be greater than after");
    }

    @Test
    void testGetMiddle() {
        String prev = "a";
        String next = "b";
        String mid = LexoRankUtil.getMiddle(prev, next);
        
        assertTrue(mid.compareTo(prev) > 0, "mid must be greater than prev");
        assertTrue(mid.compareTo(next) < 0, "mid must be less than next");

        // Edge case: varying lengths
        String prev2 = "a";
        String next2 = "a0i";
        String mid2 = LexoRankUtil.getMiddle(prev2, next2);
        assertTrue(mid2.compareTo(prev2) > 0);
        assertTrue(mid2.compareTo(next2) < 0);
        
        // Throws exception for invalid inputs
        assertThrows(IllegalArgumentException.class, () -> LexoRankUtil.getMiddle("b", "a"));
        assertThrows(IllegalArgumentException.class, () -> LexoRankUtil.getMiddle("a", "a"));
    }

    @Test
    void testSequentialInserts() {
        // Insert 20 times at the beginning
        String current = "m000000000";
        for (int i = 0; i < 20; i++) {
            String before = LexoRankUtil.getMiddle(null, current);
            assertTrue(before.compareTo(current) < 0, "Failed at iteration " + i + ": " + before + " not < " + current);
            current = before;
        }

        // Insert 20 times at the end
        current = "m000000000";
        for (int i = 0; i < 20; i++) {
            String after = LexoRankUtil.getMiddle(current, null);
            assertTrue(after.compareTo(current) > 0, "Failed at iteration " + i + ": " + after + " not > " + current);
            current = after;
        }

        // Insert 20 times exactly in the middle of two boundaries
        String left = "a";
        String right = "b";
        for (int i = 0; i < 20; i++) {
            String mid = LexoRankUtil.getMiddle(left, right);
            assertTrue(mid.compareTo(left) > 0 && mid.compareTo(right) < 0, 
                "Failed at iteration " + i + ": " + mid + " not between " + left + " and " + right);
            // shift bounds to test nested middles
            if (i % 2 == 0) {
                left = mid;
            } else {
                right = mid;
            }
        }
    }
}
