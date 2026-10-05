package com.taskcenter.model;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TaskEntityTest {

    @Test
    public void testEqualsAndHashCodeInSet() {
        Task task1 = Task.builder().title("task1").build();
        Task task2 = Task.builder().title("task2").build();

        Set<Task> taskSet = new HashSet<>();
        taskSet.add(task1);
        taskSet.add(task2);

        assertEquals(2, taskSet.size());

        task1.setId("id-1");
        task2.setId("id-2");

        assertTrue(taskSet.contains(task1));
        assertTrue(taskSet.contains(task2));
    }

    @Test
    public void testEqualsWithProxy() {
        Task task = Task.builder().id("id-1").title("task").build();
        
        // Imitate Hibernate proxy with an anonymous subclass
        Task proxy = new Task() {
            @Override
            public String getId() {
                return "id-1";
            }
        };

        // this should be true if instanceof is used
        assertTrue(task.equals(proxy), "Real entity must equal proxy with same ID");
        assertTrue(proxy.equals(task), "Proxy must equal real entity with same ID");
    }
}
