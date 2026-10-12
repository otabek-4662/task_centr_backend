package com.taskcenter.service;

import com.taskcenter.dto.TaskDto;
import com.taskcenter.dto.TaskReorderRequest;
import com.taskcenter.model.BoardColumn;
import com.taskcenter.model.Task;
import com.taskcenter.model.User;
import com.taskcenter.model.Workspace;
import com.taskcenter.repository.ColumnRepository;
import com.taskcenter.repository.TaskRepository;
import com.taskcenter.repository.UserRepository;
import com.taskcenter.repository.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("LexoRankReorderServiceTest — LexoRank qayta tartiblash mantig'i testi")
class LexoRankReorderServiceTest {

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ColumnRepository columnRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;
    private Workspace workspace;
    private BoardColumn column;

    @BeforeEach
    void setUp() {
        String uid = UUID.randomUUID().toString().substring(0, 8);
        user = userRepository.save(User.builder()
                .name("user_" + uid)
                .email("user_" + uid + "@test.com")
                .fullName("Test User")
                .password("secret123")
                .role(User.Role.USER)
                .build());

        workspace = workspaceRepository.save(Workspace.builder()
                .title("Reorder WS " + uid)
                .ownerId(user.getId())
                .keyPrefix("RO" + uid.substring(0, 2).toUpperCase())
                .build());

        column = columnRepository.save(BoardColumn.builder()
                .workspaceId(workspace.getId())
                .title("Backlog")
                .order(0)
                .build());
    }

    @Test
    @DisplayName("Vazifani o'rtaga (between) qo'yganda LexoRank to'g'ri o'zgaradi va ro'yxat tartiblanadi")
    void testReorderBetweenTasks() {
        Task t1 = taskRepository.save(Task.builder()
                .workspaceId(workspace.getId())
                .columnId(column.getId())
                .title("Task 1")
                .publicId("T-1")
                .lexoRank("0000001000")
                .build());

        Task t2 = taskRepository.save(Task.builder()
                .workspaceId(workspace.getId())
                .columnId(column.getId())
                .title("Task 2")
                .publicId("T-2")
                .lexoRank("0000002000")
                .build());

        Task t3 = taskRepository.save(Task.builder()
                .workspaceId(workspace.getId())
                .columnId(column.getId())
                .title("Task 3")
                .publicId("T-3")
                .lexoRank("0000003000")
                .build());

        // T3 ni T1 va T2 orasiga joylashtirish
        TaskReorderRequest req = TaskReorderRequest.builder()
                .columnId(column.getId())
                .prevRank(t1.getLexoRank())
                .nextRank(t2.getLexoRank())
                .build();

        TaskDto updatedT3 = taskService.reorderTask(workspace.getId(), t3.getId(), req, user);

        assertTrue(updatedT3.getLexoRank().compareTo(t1.getLexoRank()) > 0);
        assertTrue(updatedT3.getLexoRank().compareTo(t2.getLexoRank()) < 0);

        List<Task> sorted = taskRepository.findByColumnIdOrderByLexoRankAsc(column.getId());
        assertEquals(List.of("T-1", "T-3", "T-2"), sorted.stream().map(Task::getPublicId).toList());
    }
}
