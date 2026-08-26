package com.taskcenter.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "workspace_members", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"workspaceId", "userId"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(WorkspaceMemberId.class)
public class WorkspaceMember {

    @Id
    private String workspaceId;

    @Id
    private String userId;

    @Column(nullable = false)
    private String role;
}
