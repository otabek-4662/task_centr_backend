package com.taskcenter.controller;

import com.jayway.jsonpath.JsonPath;
import com.taskcenter.model.User;
import com.taskcenter.model.Workspace;
import com.taskcenter.model.BoardColumn;
import com.taskcenter.model.Task;
import com.taskcenter.repository.ColumnRepository;
import com.taskcenter.repository.TaskRepository;
import com.taskcenter.repository.UserRepository;
import com.taskcenter.repository.WorkspaceRepository;
import com.taskcenter.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@org.springframework.test.context.ActiveProfiles("test")
class ReportControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private ColumnRepository columnRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private com.taskcenter.repository.SprintRepository sprintRepository;

    private String userToken;
    private User testUser;
    private String workspaceId;
    private String sprintId;

    @BeforeEach
    void setUp() {
        testUser = userRepository.findByName("elshod").orElseThrow();
        org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth = 
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities());
        userToken = "Bearer " + jwtTokenProvider.generateToken(auth);

        Workspace ws = Workspace.builder()
                .title("Report Test WS")
                .ownerId(testUser.getId())
                .keyPrefix("RPT-123")
                .build();
        ws = workspaceRepository.save(ws);
        workspaceId = ws.getId();

        // 3 default ustun
        BoardColumn todo = columnRepository.save(BoardColumn.builder()
                .workspaceId(workspaceId).title("To Do").order(1).isDefault(true).build());
        BoardColumn inProg = columnRepository.save(BoardColumn.builder()
                .workspaceId(workspaceId).title("In Progress").order(2).isDefault(true).build());
        BoardColumn done = columnRepository.save(BoardColumn.builder()
                .workspaceId(workspaceId).title("Done").order(3).isDefault(true).build());

        com.taskcenter.model.Sprint sprint = com.taskcenter.model.Sprint.builder()
                .workspaceId(workspaceId)
                .name("Sprint 1")
                .startDate(java.time.LocalDate.now())
                .endDate(java.time.LocalDate.now().plusDays(14))
                .status(com.taskcenter.model.SprintStatus.ACTIVE)
                .build();
        sprint = sprintRepository.save(sprint);
        sprintId = sprint.getId();

        // Tasklar yaratish
        Task t1 = Task.builder()
                .workspaceId(workspaceId).columnId(todo.getId()).title("Task 1")
                .lexoRank("A").publicId("RPT-001")
                .sprintId(sprintId).storyPoints(3).build();
        t1.getAssignees().add(testUser);
        
        Task t2 = Task.builder()
                .workspaceId(workspaceId).columnId(inProg.getId()).title("Task 2")
                .lexoRank("B").publicId("RPT-002")
                .sprintId(sprintId).storyPoints(5).build();
        t2.getAssignees().add(testUser);

        Task t3 = Task.builder()
                .workspaceId(workspaceId).columnId(done.getId()).title("Task 3")
                .lexoRank("C").publicId("RPT-003")
                .sprintId(sprintId).storyPoints(2).build();
        t3.getAssignees().add(testUser);

        Task t4 = Task.builder()
                .workspaceId(workspaceId).columnId(done.getId()).title("Task 4")
                .lexoRank("D").publicId("RPT-004").build();
        // Task 4 ga hech kim biriktirilmagan va u sprintda emas

        taskRepository.saveAll(List.of(t1, t2, t3, t4));
    }

    @Test
    void testGetWorkspaceSummary() throws Exception {
        mvc.perform(get("/api/workspaces/" + workspaceId + "/reports/summary")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalTasks").value(4))
                .andExpect(jsonPath("$.data.todoTasks").value(1))
                .andExpect(jsonPath("$.data.inProgressTasks").value(1))
                .andExpect(jsonPath("$.data.doneTasks").value(2))
                .andExpect(jsonPath("$.data.completionPercentage").value(50.0)) // (2 / 4) * 100
                .andExpect(jsonPath("$.data.workload[0].userId").value(testUser.getId()))
                .andExpect(jsonPath("$.data.workload[0].totalAssignedTasks").value(3)) // t1, t2, t3
                .andExpect(jsonPath("$.data.workload[0].activeTasks").value(2)) // t1, t2
                .andExpect(jsonPath("$.data.workload[0].completedTasks").value(1)); // t3
    }

    @Test
    void testGetSprintSummary() throws Exception {
        mvc.perform(get("/api/workspaces/" + workspaceId + "/reports/sprints/" + sprintId)
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalTasks").value(3)) // t1, t2, t3
                .andExpect(jsonPath("$.data.completedTasks").value(1)) // t3 is done
                .andExpect(jsonPath("$.data.totalStoryPoints").value(10)) // 3 + 5 + 2
                .andExpect(jsonPath("$.data.completedStoryPoints").value(2)) // 2 (from t3)
                .andExpect(jsonPath("$.data.completionPercentage").value(20.0)); // 2/10 = 20%
    }
}
