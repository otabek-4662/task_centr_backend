package com.taskcenter.controller;

import com.jayway.jsonpath.JsonPath;
import com.taskcenter.model.BoardColumn;
import com.taskcenter.model.Task;
import com.taskcenter.model.User;
import com.taskcenter.repository.ColumnRepository;
import com.taskcenter.repository.TaskRepository;
import com.taskcenter.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CommentControllerTest {

    private static final String SEED_WS = "6a45163133ff7819b28ef909";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ColumnRepository columnRepository;

    @Autowired
    private TaskRepository taskRepository;

    private String taskId;

    private String tokenFor(String name) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.token");
    }

    private String bearer(String name) throws Exception {
        return "Bearer " + tokenFor(name);
    }

    @BeforeEach
    void setUp() {
        BoardColumn col = columnRepository.findByWorkspaceIdOrderByOrderAsc(SEED_WS).get(0);
        Task task = Task.builder()
                .workspaceId(SEED_WS)
                .columnId(col.getId())
                .title("Comment Test Task")
                .order(1)
                .build();
        Task saved = taskRepository.save(task);
        taskId = saved.getId();
    }

    @Test
    @DisplayName("Taskka izoh qo'shish va uni ro'yxatda ko'rish")
    void addAndGetComments() throws Exception {
        String auth = bearer("elshod");

        // 1. Yangi izoh qo'shish (201 Created)
        MvcResult postResult = mvc.perform(post("/api/tasks/" + taskId + "/comments")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Dastlabki izoh!\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").value("Dastlabki izoh!"))
                .andExpect(jsonPath("$.data.authorName").value("elshod"))
                .andReturn();

        String commentId = JsonPath.read(postResult.getResponse().getContentAsString(), "$.data.id");

        // 2. Izohlar ro'yxatini olish (Pageable)
        mvc.perform(get("/api/tasks/" + taskId + "/comments")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(commentId))
                .andExpect(jsonPath("$.data.content[0].content").value("Dastlabki izoh!"));
    }

    @Test
    @DisplayName("Muallif o'z izohini tahrirlashi mumkin")
    void updateComment_authorCanUpdate() throws Exception {
        String auth = bearer("elshod");

        MvcResult postResult = mvc.perform(post("/api/tasks/" + taskId + "/comments")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Eski matn\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        String commentId = JsonPath.read(postResult.getResponse().getContentAsString(), "$.data.id");

        mvc.perform(put("/api/tasks/" + taskId + "/comments/" + commentId)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Yangi tahrirlangan matn\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").value("Yangi tahrirlangan matn"));
    }

    @Test
    @DisplayName("Muallif o'z izohini o'chira oladi")
    void deleteComment_authorCanDelete() throws Exception {
        String auth = bearer("elshod");

        MvcResult postResult = mvc.perform(post("/api/tasks/" + taskId + "/comments")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"O'chirilishi kerak bo'lgan izoh\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        String commentId = JsonPath.read(postResult.getResponse().getContentAsString(), "$.data.id");

        mvc.perform(delete("/api/tasks/" + taskId + "/comments/" + commentId)
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
