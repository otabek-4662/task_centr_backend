package com.taskcenter.model;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LabelEntityTest {

    @Test
    public void testEqualsAndHashCodeInSet() {
        Label label1 = Label.builder().name("label1").build();
        Label label2 = Label.builder().name("label2").build();

        Set<Label> labelSet = new HashSet<>();
        labelSet.add(label1);
        labelSet.add(label2);

        assertEquals(2, labelSet.size());

        label1.setId("id-1");
        label2.setId("id-2");

        assertTrue(labelSet.contains(label1));
        assertTrue(labelSet.contains(label2));
    }

    @Test
    public void testEqualsWithProxy() {
        Label label = Label.builder().id("id-1").name("label").build();
        
        Label proxy = new Label() {
            @Override
            public String getId() {
                return "id-1";
            }
        };

        assertTrue(label.equals(proxy), "Real entity must equal proxy with same ID");
        assertTrue(proxy.equals(label), "Proxy must equal real entity with same ID");
    }
}
