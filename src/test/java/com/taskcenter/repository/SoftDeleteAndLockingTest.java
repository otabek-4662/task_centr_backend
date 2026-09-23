package com.taskcenter.repository;

import com.taskcenter.model.BoardColumn;
import com.taskcenter.model.Task;
import com.taskcenter.model.User;
import com.taskcenter.model.Workspace;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class SoftDeleteAndLockingTest {

    @Autowired private TaskRepository taskRepository;
    @Autowired private ColumnRepository columnRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private EntityManager em;

    private Workspace workspace;
    private BoardColumn column;

    @BeforeEach
    void seed() {
        User owner = userRepository.save(User.builder()
                .name("lock-owner").password("x").role(User.Role.USER).build());
        workspace = workspaceRepository.save(Workspace.builder()
                .title("Lock WS").ownerId(owner.getId()).build());
        column = columnRepository.save(BoardColumn.builder()
                .workspaceId(workspace.getId()).title("Col").order(1).build());
    }

    @Test
    void task_softDelete_hidesFromFindById() {
        Task task = taskRepository.save(Task.builder()
                .workspaceId(workspace.getId()).columnId(column.getId())
                .publicId("SD-1").title("to-delete").lexoRank("0000000001").build());
        taskRepository.flush();
        em.clear();

        taskRepository.deleteById(task.getId());
        taskRepository.flush();
        em.clear();

        assertThat(taskRepository.findById(task.getId())).isEmpty();
    }

    @Test
    void column_softDelete_hidesFromFindById() {
        BoardColumn col = columnRepository.save(BoardColumn.builder()
                .workspaceId(workspace.getId()).title("to-delete").order(5).build());
        columnRepository.flush();
        em.clear();

        columnRepository.deleteById(col.getId());
        columnRepository.flush();
        em.clear();

        assertThat(columnRepository.findById(col.getId())).isEmpty();
    }

    @Test
    void column_softDelete_doesNotAffectOtherColumns() {
        BoardColumn keep = columnRepository.save(BoardColumn.builder()
                .workspaceId(workspace.getId()).title("keep").order(10).build());
        BoardColumn remove = columnRepository.save(BoardColumn.builder()
                .workspaceId(workspace.getId()).title("remove").order(11).build());
        columnRepository.flush();
        em.clear();

        columnRepository.deleteById(remove.getId());
        columnRepository.flush();
        em.clear();

        assertThat(columnRepository.findById(keep.getId())).isPresent();
        assertThat(columnRepository.findById(remove.getId())).isEmpty();
    }

    @Test
    void task_optimisticLocking_throwsOnConcurrentUpdate() {
        Task task = taskRepository.save(Task.builder()
                .workspaceId(workspace.getId()).columnId(column.getId())
                .publicId("LK-1").title("original").lexoRank("0000000001").build());
        taskRepository.flush();
        em.clear();

        Task loaded = taskRepository.findById(task.getId()).orElseThrow();

        em.createNativeQuery(
                "UPDATE tasks SET title = 'concurrent', version = version + 1 WHERE id = ?")
                .setParameter(1, task.getId())
                .executeUpdate();

        loaded.setTitle("stale-update");
        assertThatThrownBy(() -> taskRepository.saveAndFlush(loaded))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    void task_savedTwice_noException_withoutVersionChange() {
        Task task = taskRepository.save(Task.builder()
                .workspaceId(workspace.getId()).columnId(column.getId())
                .publicId("OK-1").title("ok").lexoRank("0000000001").build());
        taskRepository.flush();
        em.clear();

        Task fetched = taskRepository.findById(task.getId()).orElseThrow();
        fetched.setTitle("updated-ok");
        taskRepository.saveAndFlush(fetched);
        em.clear();

        assertThat(taskRepository.findById(task.getId()))
                .isPresent()
                .hasValueSatisfying(t -> assertThat(t.getTitle()).isEqualTo("updated-ok"));
    }

    @Test
    void workspace_softDelete_hidesFromFindById() {
        Workspace ws = workspaceRepository.save(Workspace.builder()
                .title("to-delete").ownerId(workspace.getOwnerId()).build());
        workspaceRepository.flush();
        em.clear();

        workspaceRepository.deleteById(ws.getId());
        workspaceRepository.flush();
        em.clear();

        assertThat(workspaceRepository.findById(ws.getId())).isEmpty();
    }

    @Test
    void workspace_softDelete_doesNotAffectOwnerQuery() {
        Workspace ws = workspaceRepository.save(Workspace.builder()
                .title("ws-keep").ownerId(workspace.getOwnerId()).build());
        workspaceRepository.flush();
        em.clear();

        workspaceRepository.deleteById(ws.getId());
        workspaceRepository.flush();
        em.clear();

        assertThat(workspaceRepository.findByOwnerIdOrMemberUserId(workspace.getOwnerId()))
                .extracting(Workspace::getId)
                .doesNotContain(ws.getId());
    }
}
