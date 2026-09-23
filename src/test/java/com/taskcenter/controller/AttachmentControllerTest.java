package com.taskcenter.controller;

import com.jayway.jsonpath.JsonPath;
import com.taskcenter.model.BoardColumn;
import com.taskcenter.model.Task;
import com.taskcenter.repository.ColumnRepository;
import com.taskcenter.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
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
class AttachmentControllerTest {

    private static final String SEED_WS = "6a45163133ff7819b28ef909";

    @Autowired
    private MockMvc mvc;

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
                .title("Attachment Test Task")
                .lexoRank("0000000001")
                .build();
        Task saved = taskRepository.save(task);
        taskId = saved.getId();
    }

    @Test
    @DisplayName("Fayl yuklash, yuklab olish va o'chirish lifecycle testi")
    void attachmentLifecycle() throws Exception {
        String auth = bearer("elshod");
        MockMultipartFile file = new MockMultipartFile(
                "file", "test-doc.txt", "text/plain", "Hello Antigravity Attachment!".getBytes());

        // 1. Faylni yuklash (201 Created)
        MvcResult uploadResult = mvc.perform(multipart("/api/tasks/" + taskId + "/attachments")
                        .file(file)
                        .header("Authorization", auth))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fileName").value("test-doc.txt"))
                .andExpect(jsonPath("$.data.uploaderName").value("elshod"))
                .andReturn();

        String attachmentId = JsonPath.read(uploadResult.getResponse().getContentAsString(), "$.data.id");

        // 2. Fayllar ro'yxatini olish
        mvc.perform(get("/api/tasks/" + taskId + "/attachments")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(attachmentId));

        // 3. Faylni yuklab olish
        mvc.perform(get("/api/attachments/" + attachmentId + "/download")
                        .header("Authorization", auth))
                .andExpect(status().isOk());

        // 4. Faylni o'chirish
        mvc.perform(delete("/api/attachments/" + attachmentId)
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
