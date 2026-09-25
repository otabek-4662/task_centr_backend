package com.taskcenter.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskcenter.dto.GitHubPushEventDto;
import com.taskcenter.model.BoardColumn;
import com.taskcenter.model.Sprint;
import com.taskcenter.model.SprintStatus;
import com.taskcenter.model.Task;
import com.taskcenter.model.User;
import com.taskcenter.model.Workspace;
import com.taskcenter.repository.ColumnRepository;
import com.taskcenter.repository.SprintRepository;
import com.taskcenter.repository.TaskRepository;
import com.taskcenter.repository.UserRepository;
import com.taskcenter.repository.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@org.springframework.test.context.ActiveProfiles("test")
class WebhookControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private ColumnRepository columnRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private Workspace ws;
    private BoardColumn doneColumn;
    private Task testTask;

    @BeforeEach
    void setUp() {
        testUser = userRepository.findByName("elshod").orElseThrow();

        ws = Workspace.builder()
                .title("Webhook Test WS")
                .ownerId(testUser.getId())
                .keyPrefix("WFM")
                .build();
        ws = workspaceRepository.save(ws);

        BoardColumn todoColumn = columnRepository.save(BoardColumn.builder()
                .workspaceId(ws.getId()).title("To Do").order(1).isDefault(true).build());
        doneColumn = columnRepository.save(BoardColumn.builder()
                .workspaceId(ws.getId()).title("Done").order(3).isDefault(true).build());

        testTask = Task.builder()
                .workspaceId(ws.getId()).columnId(todoColumn.getId()).title("Webhook Task")
                .lexoRank("A").publicId("WFM-99").build();
        testTask = taskRepository.save(testTask);
    }

    @Test
    void testGitHubPushEvent() throws Exception {
        GitHubPushEventDto.Commit commit = new GitHubPushEventDto.Commit();
        commit.setId("commit123");
        commit.setMessage("Fixes WFM-99 and does some other stuff");
        commit.setUrl("https://github.com/test/commit/123");
        
        GitHubPushEventDto.Author author = new GitHubPushEventDto.Author();
        author.setName("Test Author");
        commit.setAuthor(author);

        GitHubPushEventDto.Repository repo = new GitHubPushEventDto.Repository();
        repo.setName("test-repo");
        repo.setFull_name("test/test-repo");

        GitHubPushEventDto payload = new GitHubPushEventDto();
        payload.setCommits(List.of(commit));
        payload.setRepository(repo);

        mvc.perform(post("/api/webhooks/github")
                        .header("X-GitHub-Event", "push")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        // Ma'lumotlarni bazadan tekshiramiz
        Optional<Task> updatedTaskOpt = taskRepository.findByPublicId("WFM-99");
        assertEquals(true, updatedTaskOpt.isPresent());
        assertEquals(doneColumn.getId(), updatedTaskOpt.get().getColumnId());
    }
}
