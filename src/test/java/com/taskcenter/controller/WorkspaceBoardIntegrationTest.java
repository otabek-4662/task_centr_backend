package com.taskcenter.controller;

import com.jayway.jsonpath.JsonPath;
import com.taskcenter.model.*;
import com.taskcenter.repository.ColumnRepository;
import com.taskcenter.repository.TaskRepository;
import com.taskcenter.repository.UserRepository;
import com.taskcenter.repository.WorkspaceMemberRepository;
import com.taskcenter.repository.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WorkspaceBoardIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberRepository memberRepository;

    @Autowired
    private ColumnRepository columnRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User owner;
    private User nonMember;
    private User assigneeUser;
    private Workspace workspace;
    private BoardColumn col1;
    private BoardColumn col2;
    private Task task1;
    private Task task2;
    private Task task3;

    private String login(String name, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return "Bearer " + JsonPath.read(result.getResponse().getContentAsString(), "$.data.token");
    }

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String pass = "password123";

        owner = userRepository.save(User.builder()
                .name("owner_" + suffix)
                .fullName("Owner Developer")
                .email("owner_" + suffix + "@example.com")
                .password(passwordEncoder.encode(pass))
                .role(User.Role.USER)
                .build());

        nonMember = userRepository.save(User.builder()
                .name("nonmember_" + suffix)
                .fullName("Stranger User")
                .email("nonmember_" + suffix + "@example.com")
                .password(passwordEncoder.encode(pass))
                .role(User.Role.USER)
                .build());

        assigneeUser = userRepository.save(User.builder()
                .name("assignee_" + suffix)
                .fullName("Assignee Expert")
                .email("assignee_" + suffix + "@example.com")
                .password(passwordEncoder.encode(pass))
                .role(User.Role.USER)
                .build());

        workspace = workspaceRepository.save(Workspace.builder()
                .title("Board Test Workspace " + suffix)
                .ownerId(owner.getId())
                .build());

        memberRepository.save(WorkspaceMember.builder()
                .workspaceId(workspace.getId())
                .userId(owner.getId())
                .role(WorkspaceRole.OWNER)
                .build());

        memberRepository.save(WorkspaceMember.builder()
                .workspaceId(workspace.getId())
                .userId(assigneeUser.getId())
                .role(WorkspaceRole.MEMBER)
                .build());

        // 2 ta ustun yaratamiz: To Do (order 1), In Progress (order 2)
        col1 = columnRepository.save(BoardColumn.builder()
                .workspaceId(workspace.getId())
                .title("To Do")
                .order(1)
                .isDefault(true)
                .build());

        col2 = columnRepository.save(BoardColumn.builder()
                .workspaceId(workspace.getId())
                .title("In Progress")
                .order(2)
                .isDefault(false)
                .build());

        LocalDateTime now = LocalDateTime.now();

        // 3 ta task:
        // Task 1: To Do, rank 0|hzzzza:, assignee mavjud
        task1 = Task.builder()
                .workspaceId(workspace.getId())
                .columnId(col1.getId())
                .publicId("WFM-201")
                .title("Frontend arxitekturasi")
                .description("Frontend asosiy arxitekturasini tuzish")
                .priority(Priority.HIGH)
                .issueType(IssueType.STORY)
                .storyPoints(5)
                .lexoRank("0|hzzzza:")
                .createdBy(owner.getId())
                .createdAt(now.minusHours(2))
                .updatedAt(now.minusHours(1))
                .build();
        task1.getAssignees().add(assigneeUser);
        taskRepository.save(task1);

        // Task 2: To Do, rank 0|hzzzzb: (Task 1 dan keyin), assignee mavjud
        task2 = Task.builder()
                .workspaceId(workspace.getId())
                .columnId(col1.getId())
                .publicId("WFM-202")
                .title("API integratsiyasi")
                .description("Backend bilan integratsiya qilish")
                .priority(Priority.MEDIUM)
                .issueType(IssueType.TASK)
                .storyPoints(3)
                .lexoRank("0|hzzzzb:")
                .createdBy(owner.getId())
                .createdAt(now.minusHours(3))
                .updatedAt(now.minusHours(2))
                .build();
        task2.getAssignees().add(assigneeUser);
        taskRepository.save(task2);

        // Task 3: In Progress, rank 0|hzzzza:, assignee yo'q
        task3 = Task.builder()
                .workspaceId(workspace.getId())
                .columnId(col2.getId())
                .publicId("WFM-203")
                .title("E2E testlar yozish")
                .description("Cypress yoki Playwright testlar")
                .priority(Priority.URGENT)
                .issueType(IssueType.BUG)
                .storyPoints(8)
                .lexoRank("0|hzzzza:")
                .createdBy(owner.getId())
                .createdAt(now.minusHours(4))
                .updatedAt(now.minusHours(3))
                .build();
        taskRepository.save(task3);
    }

    @Test
    @DisplayName("Integratsion test: 2 ustun, 3 task, to'g'ri tartib va barcha TaskDto maydonlari")
    void testGetBoard_returnsColumnsAndTasksSortedWithAllFields() throws Exception {
        String token = login(owner.getName(), "password123");

        mvc.perform(get("/api/workspaces/" + workspace.getId() + "/board")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                // 1-ustun tekshiruvi: To Do (order=1)
                .andExpect(jsonPath("$.data[0].id").value(col1.getId()))
                .andExpect(jsonPath("$.data[0].name").value("To Do"))
                .andExpect(jsonPath("$.data[0].title").value("To Do"))
                .andExpect(jsonPath("$.data[0].position").value(1))
                .andExpect(jsonPath("$.data[0].order").value(1))
                .andExpect(jsonPath("$.data[0].rank").value("1"))
                .andExpect(jsonPath("$.data[0].tasks.length()").value(2))
                .andExpect(jsonPath("$.data[0].cards.length()").value(2))
                // 1-ustun ichidagi 1-task (rank bo'yicha Task 1: 0|hzzzza:)
                .andExpect(jsonPath("$.data[0].tasks[0].id").value(task1.getId()))
                .andExpect(jsonPath("$.data[0].tasks[0].key").value("WFM-201"))
                .andExpect(jsonPath("$.data[0].tasks[0].code").value("WFM-201"))
                .andExpect(jsonPath("$.data[0].tasks[0].title").value("Frontend arxitekturasi"))
                .andExpect(jsonPath("$.data[0].tasks[0].description").value("Frontend asosiy arxitekturasini tuzish"))
                .andExpect(jsonPath("$.data[0].tasks[0].priority").value("HIGH"))
                .andExpect(jsonPath("$.data[0].tasks[0].issueType").value("STORY"))
                .andExpect(jsonPath("$.data[0].tasks[0].storyPoints").value(5))
                .andExpect(jsonPath("$.data[0].tasks[0].rank").value("0|hzzzza:"))
                .andExpect(jsonPath("$.data[0].tasks[0].columnId").value(col1.getId()))
                .andExpect(jsonPath("$.data[0].tasks[0].assignee.id").value(assigneeUser.getId()))
                .andExpect(jsonPath("$.data[0].tasks[0].assignee.fullName").value("Assignee Expert"))
                .andExpect(jsonPath("$.data[0].tasks[0].reporter.id").value(owner.getId()))
                .andExpect(jsonPath("$.data[0].tasks[0].reporter.fullName").value("Owner Developer"))
                .andExpect(jsonPath("$.data[0].tasks[0].createdAt").isNotEmpty())
                .andExpect(jsonPath("$.data[0].tasks[0].updatedAt").isNotEmpty())
                // 1-ustun ichidagi 2-task (rank bo'yicha Task 2: 0|hzzzzb:)
                .andExpect(jsonPath("$.data[0].tasks[1].id").value(task2.getId()))
                .andExpect(jsonPath("$.data[0].tasks[1].key").value("WFM-202"))
                .andExpect(jsonPath("$.data[0].tasks[1].code").value("WFM-202"))
                .andExpect(jsonPath("$.data[0].tasks[1].title").value("API integratsiyasi"))
                .andExpect(jsonPath("$.data[0].tasks[1].priority").value("MEDIUM"))
                .andExpect(jsonPath("$.data[0].tasks[1].issueType").value("TASK"))
                .andExpect(jsonPath("$.data[0].tasks[1].storyPoints").value(3))
                .andExpect(jsonPath("$.data[0].tasks[1].rank").value("0|hzzzzb:"))
                .andExpect(jsonPath("$.data[0].tasks[1].columnId").value(col1.getId()))
                // 2-ustun tekshiruvi: In Progress (order=2)
                .andExpect(jsonPath("$.data[1].id").value(col2.getId()))
                .andExpect(jsonPath("$.data[1].name").value("In Progress"))
                .andExpect(jsonPath("$.data[1].title").value("In Progress"))
                .andExpect(jsonPath("$.data[1].position").value(2))
                .andExpect(jsonPath("$.data[1].order").value(2))
                .andExpect(jsonPath("$.data[1].tasks.length()").value(1))
                // 2-ustun ichidagi task (Task 3)
                .andExpect(jsonPath("$.data[1].tasks[0].id").value(task3.getId()))
                .andExpect(jsonPath("$.data[1].tasks[0].key").value("WFM-203"))
                .andExpect(jsonPath("$.data[1].tasks[0].priority").value("URGENT"))
                .andExpect(jsonPath("$.data[1].tasks[0].issueType").value("BUG"))
                .andExpect(jsonPath("$.data[1].tasks[0].storyPoints").value(8))
                .andExpect(jsonPath("$.data[1].tasks[0].columnId").value(col2.getId()))
                .andExpect(jsonPath("$.data[1].tasks[0].reporter.id").value(owner.getId()))
                .andExpect(jsonPath("$.data[1].tasks[0].reporter.fullName").value("Owner Developer"));
    }

    @Test
    @DisplayName("Ko'rish ruxsati: workspace a'zosi bo'lmagan foydalanuvchi 403 Forbidden oladi")
    void testGetBoard_nonMemberForbidden() throws Exception {
        String nonMemberToken = login(nonMember.getName(), "password123");

        mvc.perform(get("/api/workspaces/" + workspace.getId() + "/board")
                        .header("Authorization", nonMemberToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Ko'rish ruxsati: autentifikatsiyasiz so'rov 401 Unauthorized oladi")
    void testGetBoard_unauthenticatedReturns401() throws Exception {
        mvc.perform(get("/api/workspaces/" + workspace.getId() + "/board"))
                .andExpect(status().isUnauthorized());
    }
}
