package com.taskcenter.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "workspace_members", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"workspaceId", "userId"})
}, indexes = {
    @Index(name = "idx_workspace_members_user_id", columnList = "userId")
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkspaceRole role;
}
