package com.taskcenter.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "labels")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Label {

    @Id
    private String id;

    @Column(nullable = false)
    private String workspaceId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String color;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        }
    }
}
