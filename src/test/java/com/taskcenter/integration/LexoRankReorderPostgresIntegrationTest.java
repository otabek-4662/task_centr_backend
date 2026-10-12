package com.taskcenter.integration;

import com.taskcenter.dto.TaskDto;
import com.taskcenter.dto.TaskReorderRequest;
import com.taskcenter.model.BoardColumn;
import com.taskcenter.model.IssueType;
import com.taskcenter.model.Priority;
import com.taskcenter.model.Task;
import com.taskcenter.model.User;
import com.taskcenter.model.Workspace;
import com.taskcenter.repository.ColumnRepository;
import com.taskcenter.repository.TaskRepository;
import com.taskcenter.repository.UserRepository;
import com.taskcenter.repository.WorkspaceRepository;
import com.taskcenter.service.TaskService;
import com.taskcenter.util.LexoRankUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testcontainers PostgreSQL yordamida vazifalarning (Tasks) o'rnini almashtirganda
 * LexoRank algoritmi asosida ma'lumotlar bazasida to'g'ri tartiblanishini (ORDER BY lexoRank ASC)
 * tekshiruvchi integratsion test.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@EnabledIf("isDockerAvailable")
class LexoRankReorderPostgresIntegrationTest {

    static boolean isDockerAvailable() {
        boolean isCi = "true".equalsIgnoreCase(System.getenv("CI")) || "true".equalsIgnoreCase(System.getProperty("CI"));
        try {
            boolean available = DockerClientFactory.instance().isDockerAvailable();
            if (!available && isCi) {
                org.junit.jupiter.api.Assertions.fail("CI muhitida (CI=true) Docker mavjud bo'lishi shart, lekin Testcontainers Docker'ni topa olmadi");
            }
            return available;
        } catch (org.opentest4j.AssertionFailedError afe) {
            throw afe;
        } catch (Throwable t) {
            if (isCi) {
                org.junit.jupiter.api.Assertions.fail("CI muhitida (CI=true) Docker mavjud bo'lishi shart, lekin xatolik yuz berdi: " + t.getMessage());
            }
            return false;
        }
    }

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.flyway.baseline-on-migrate", () -> "false");
    }

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User testUser;
    private Workspace testWorkspace;
    private BoardColumn colTodo;
    private BoardColumn colDone;

    @BeforeEach
    void setUp() {
        String uid = UUID.randomUUID().toString().substring(0, 8);
        testUser = userRepository.save(User.builder()
                .name("lexo_user_" + uid)
                .email("lexo_" + uid + "@test.com")
                .fullName("LexoRank Test User")
                .password("pass123")
                .role(User.Role.USER)
                .build());

        testWorkspace = workspaceRepository.save(Workspace.builder()
                .title("LexoRank Workspace " + uid)
                .ownerId(testUser.getId())
                .keyPrefix("LX" + uid.substring(0, 2).toUpperCase())
                .build());

        colTodo = columnRepository.save(BoardColumn.builder()
                .workspaceId(testWorkspace.getId())
                .title("To Do")
                .order(0)
                .isDefault(true)
                .build());

        colDone = columnRepository.save(BoardColumn.builder()
                .workspaceId(testWorkspace.getId())
                .title("Done")
                .order(1)
                .isDone(true)
                .build());
    }

    private Task createTask(String title, String publicId, String lexoRank, String columnId) {
        return taskRepository.save(Task.builder()
                .workspaceId(testWorkspace.getId())
                .columnId(columnId)
                .title(title)
                .publicId(publicId)
                .lexoRank(lexoRank)
                .priority(Priority.MEDIUM)
                .issueType(IssueType.TASK)
                .build());
    }

    @Test
    @DisplayName("PostgreSQL: Ikki vazifa orasiga joylashtirilganda (Between) LexoRank to'g'ri oraliqni oladi va tartib to'g'ri saqlanadi")
    void testReorderBetweenTasks_LexoRankOrdering() {
        // 1. Dastlabki 3 ta vazifa: A, B, C (LexoRank: 1000, 2000, 3000)
        Task taskA = createTask("Task A", "LX-1", "0000001000", colTodo.getId());
        Task taskB = createTask("Task B", "LX-2", "0000002000", colTodo.getId());
        Task taskC = createTask("Task C", "LX-3", "0000003000", colTodo.getId());

        // Boshlang'ich tartib: [A, B, C]
        List<Task> initialList = taskRepository.findByColumnIdOrderByLexoRankAsc(colTodo.getId());
        assertEquals(List.of("LX-1", "LX-2", "LX-3"),
                initialList.stream().map(Task::getPublicId).toList());

        // 2. Task C ni Task A va Task B ning orasiga ko'chiramiz:
        // prevRank = taskA.lexoRank ("0000001000"), nextRank = taskB.lexoRank ("0000002000")
        TaskReorderRequest reorderReq = TaskReorderRequest.builder()
                .columnId(colTodo.getId())
                .prevRank(taskA.getLexoRank())
                .nextRank(taskB.getLexoRank())
                .build();

        TaskDto updatedC = taskService.reorderTask(testWorkspace.getId(), taskC.getId(), reorderReq, testUser);

        // Yangi LexoRank A dan katta va B dan kichik bo'lishi kerak:
        assertTrue(updatedC.getLexoRank().compareTo(taskA.getLexoRank()) > 0,
                "Yangi rank Task A dan keyin bo'lishi kerak");
        assertTrue(updatedC.getLexoRank().compareTo(taskB.getLexoRank()) < 0,
                "Yangi rank Task B dan oldin bo'lishi kerak");

        // PostgreSQL dan SELECT ... ORDER BY task_order ASC orqali tekshirish:
        List<Task> reorderedList = taskRepository.findByColumnIdOrderByLexoRankAsc(colTodo.getId());
        assertEquals(List.of("LX-1", "LX-3", "LX-2"),
                reorderedList.stream().map(Task::getPublicId).toList(),
                "PostgreSQL da tartib [Task A, Task C, Task B] bo'lishi shart");
    }

    @Test
    @DisplayName("PostgreSQL: Eng yuqoriga (Top / Before First) ko'chirilganda LexoRank eng kichik bo'ladi")
    void testReorderToTop_LexoRankOrdering() {
        Task taskA = createTask("Task A", "LX-10", "0000001000", colTodo.getId());
        Task taskB = createTask("Task B", "LX-20", "0000002000", colTodo.getId());
        Task taskC = createTask("Task C", "LX-30", "0000003000", colTodo.getId());

        // Task C ni eng yuqoriga (Task A dan ham oldinga) ko'chirish:
        // prevRank = null, nextRank = taskA.lexoRank
        TaskReorderRequest reorderReq = TaskReorderRequest.builder()
                .columnId(colTodo.getId())
                .prevRank(null)
                .nextRank(taskA.getLexoRank())
                .build();

        TaskDto updatedC = taskService.reorderTask(testWorkspace.getId(), taskC.getId(), reorderReq, testUser);

        // Task C ning LexoRank qiymati Task A dan kichik bo'lishi kerak
        assertTrue(updatedC.getLexoRank().compareTo(taskA.getLexoRank()) < 0);

        List<Task> result = taskRepository.findByColumnIdOrderByLexoRankAsc(colTodo.getId());
        assertEquals(List.of("LX-30", "LX-10", "LX-20"),
                result.stream().map(Task::getPublicId).toList(),
                "PostgreSQL da tartib [Task C, Task A, Task B] bo'lishi shart");
    }

    @Test
    @DisplayName("PostgreSQL: Eng oxiriga (Bottom / After Last) ko'chirilganda LexoRank eng katta bo'ladi")
    void testReorderToBottom_LexoRankOrdering() {
        Task taskA = createTask("Task A", "LX-100", "0000001000", colTodo.getId());
        Task taskB = createTask("Task B", "LX-200", "0000002000", colTodo.getId());
        Task taskC = createTask("Task C", "LX-300", "0000003000", colTodo.getId());

        // Task A ni eng pastga (Task C dan ham keyinga) ko'chirish:
        // prevRank = taskC.lexoRank, nextRank = null
        TaskReorderRequest reorderReq = TaskReorderRequest.builder()
                .columnId(colTodo.getId())
                .prevRank(taskC.getLexoRank())
                .nextRank(null)
                .build();

        TaskDto updatedA = taskService.reorderTask(testWorkspace.getId(), taskA.getId(), reorderReq, testUser);

        assertTrue(updatedA.getLexoRank().compareTo(taskC.getLexoRank()) > 0);

        List<Task> result = taskRepository.findByColumnIdOrderByLexoRankAsc(colTodo.getId());
        assertEquals(List.of("LX-200", "LX-300", "LX-100"),
                result.stream().map(Task::getPublicId).toList(),
                "PostgreSQL da tartib [Task B, Task C, Task A] bo'lishi shart");
    }

    @Test
    @DisplayName("PostgreSQL: Boshqa ustunga ko'chirilganda (Cross-column) ustun o'zgaradi va yangi ustunda to'g'ri joylashadi")
    void testReorderCrossColumn_LexoRankOrdering() {
        Task taskA = createTask("Task A", "LX-A", "0000001000", colTodo.getId());
        Task taskB = createTask("Task B", "LX-B", "0000002000", colTodo.getId());

        Task taskDone1 = createTask("Done 1", "LX-D1", "0000001000", colDone.getId());
        Task taskDone2 = createTask("Done 2", "LX-D2", "0000002000", colDone.getId());

        // Task A ni colTodo dan colDone ga, Done 1 va Done 2 orasiga ko'chiramiz:
        TaskReorderRequest reorderReq = TaskReorderRequest.builder()
                .columnId(colDone.getId())
                .prevRank(taskDone1.getLexoRank())
                .nextRank(taskDone2.getLexoRank())
                .build();

        taskService.reorderTask(testWorkspace.getId(), taskA.getId(), reorderReq, testUser);

        // colTodo da faqat Task B qolishi kerak:
        List<Task> todoTasks = taskRepository.findByColumnIdOrderByLexoRankAsc(colTodo.getId());
        assertEquals(List.of("LX-B"), todoTasks.stream().map(Task::getPublicId).toList());

        // colDone da tartib [Done 1, Task A, Done 2] bo'lishi kerak:
        List<Task> doneTasks = taskRepository.findByColumnIdOrderByLexoRankAsc(colDone.getId());
        assertEquals(List.of("LX-D1", "LX-A", "LX-D2"),
                doneTasks.stream().map(Task::getPublicId).toList());
    }
}
