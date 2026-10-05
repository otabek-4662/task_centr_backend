package com.taskcenter.model;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UserEntityTest {

    @Test
    public void testEqualsAndHashCodeInSet() {
        // Ikkita saqlanmagan (id=null) User bitta Set'ga qo'shilganda ikkalasi ham qolishi
        User user1 = User.builder().name("user1").build();
        User user2 = User.builder().name("user2").build();

        Set<User> userSet = new HashSet<>();
        userSet.add(user1);
        userSet.add(user2);

        assertEquals(2, userSet.size(), "Ikkita id=null bolgan farqli userlar Set'da qolishi kerak");

        // Saqlangandan keyin ham Set'da topilishi
        // Simulate save by setting IDs
        user1.setId("id-1");
        user2.setId("id-2");

        assertTrue(userSet.contains(user1), "Saqlangandan (id o'zgargandan) keyin ham birinchi user topilishi kerak");
        assertTrue(userSet.contains(user2), "Saqlangandan keyin ham ikkinchi user topilishi kerak");
    }
}
