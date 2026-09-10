package com.taskcenter.repository;

import com.taskcenter.model.BoardColumn;
import com.taskcenter.model.Task;
import com.taskcenter.model.User;
import com.taskcenter.model.Workspace;
import com.taskcenter.model.WorkspaceMember;
import com.taskcenter.model.WorkspaceRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class WorkspaceQueriesTest {

    @Autowired
    private WorkspaceRepository workspaceRepository;
    @Autowired
    private WorkspaceMemberRepository memberRepository;
    @Autowired
    private ColumnRepository columnRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private UserRepository userRepository;

    private User owner;
    private User member;
    private Workspace workspace;

    @BeforeEach
    void seed() {
        owner = userRepository.save(User.builder()
                .name("repo-owner").password("x").role(User.Role.USER).build());
        member = userRepository.save(User.builder()
                .name("repo-member").password("x").role(User.Role.USER).build());
        userRepository.save(User.builder()
                .name("repo-stranger").password("x").role(User.Role.USER).build());

        workspace = workspaceRepository.save(Workspace.builder()
                .title("Repo WS").ownerId(owner.getId()).build());
        memberRepository.save(WorkspaceMember.builder()
                .workspaceId(workspace.getId()).userId(member.getId()).role(WorkspaceRole.MEMBER).build());
    }

    @Test
    void findByOwnerIdOrMemberUserId_returnsWorkspaceForOwnerAndMember() {
        List<Workspace> forOwner =
                workspaceRepository.findByOwnerIdOrMemberUserId(owner.getId());
        List<Workspace> forMember =
                workspaceRepository.findByOwnerIdOrMemberUserId(member.getId());

        assertThat(forOwner).extracting(Workspace::getId).contains(workspace.getId());
        assertThat(forMember).extracting(Workspace::getId).contains(workspace.getId());
    }

    @Test
    void findByOwnerIdOrMemberUserId_returnsEmptyForStranger() {
        User stranger = userRepository.findByName("repo-stranger").orElseThrow();

        assertThat(workspaceRepository.findByOwnerIdOrMemberUserId(stranger.getId())).isEmpty();
    }

    @Test
    void findMaxOrderByWorkspaceId_emptyIsZeroThenMax() {
        assertThat(columnRepository.findMaxOrderByWorkspaceId(workspace.getId())).isZero();

        columnRepository.save(BoardColumn.builder()
                .workspaceId(workspace.getId()).title("A").order(1).build());
        columnRepository.save(BoardColumn.builder()
                .workspaceId(workspace.getId()).title("B").order(2).build());

        assertThat(columnRepository.findMaxOrderByWorkspaceId(workspace.getId())).isEqualTo(2);
    }

    @Test
    void findMaxOrderByColumnId_tracksTasksInsideColumn() {
        BoardColumn col = columnRepository.save(BoardColumn.builder()
                .workspaceId(workspace.getId()).title("A").order(1).build());

        assertThat(taskRepository.findMaxOrderByColumnId(col.getId())).isZero();

        taskRepository.save(Task.builder()
                .workspaceId(workspace.getId()).columnId(col.getId())
                .publicId("WFM-1").title("T1").order(1).build());
        taskRepository.save(Task.builder()
                .workspaceId(workspace.getId()).columnId(col.getId())
                .publicId("WFM-2").title("T2").order(4).build());

        assertThat(taskRepository.findMaxOrderByColumnId(col.getId())).isEqualTo(4);
    }
}
