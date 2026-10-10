package com.taskcenter.controller;

import com.jayway.jsonpath.JsonPath;
import com.taskcenter.model.User;
import com.taskcenter.repository.UserRepository;
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

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TaskControllerV2Test {

    private static final String SEED_WS = "6a45163133ff7819b28ef909";
    private static final String SEED_COL = "6a45163133ff7819b28ef90d";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User elshod;
    private User xusan;

    @BeforeEach
    void setUp() {
        xusan = userRepository.findByNameOrEmail("xusan").orElseThrow();
        elshod = userRepository.findByNameOrEmail("elshod").orElseThrow();
    }

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

    @Test
    @DisplayName("createTaskV2: Muvaffaqiyatli yangi vazifa yaratish va to'liq ma'lumotlarni qaytarish (success)")
    void createTaskV2_success() throws Exception {
        String payload = """
                {
                  "columnId": "%s",
                  "title": "v2 Yangi Bosh Og'riq",
                  "description": "API v2 orqali yaratilgan sinov vazifasi",
                  "priority": "HIGH",
                  "issueType": "TASK",
                  "dueDate": "2026-11-20",
                  "storyPoints": 8,
                  "direction": ["Backend", "Frontend"],
                  "users": ["%s"]
                }
                """.formatted(SEED_COL, elshod.getId());

        mvc.perform(post("/api/v2/workspaces/" + SEED_WS + "/tasks")
                        .header("Authorization", bearer("xusan"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.publicId").isNotEmpty())
                .andExpect(jsonPath("$.data.columnId").value(SEED_COL))
                .andExpect(jsonPath("$.data.title").value("v2 Yangi Bosh Og'riq"))
                .andExpect(jsonPath("$.data.description").value("API v2 orqali yaratilgan sinov vazifasi"))
                .andExpect(jsonPath("$.data.priority").value("HIGH"))
                .andExpect(jsonPath("$.data.issueType").value("TASK"))
                .andExpect(jsonPath("$.data.dueDate").value("2026-11-20"))
                .andExpect(jsonPath("$.data.storyPoints").value(8))
                .andExpect(jsonPath("$.data.direction", hasItems("Backend", "Frontend")))
                .andExpect(jsonPath("$.data.users", hasSize(1)))
                .andExpect(jsonPath("$.data.users[0].id").value(elshod.getId()))
                .andExpect(jsonPath("$.data.users[0].name").value("elshod"))
                .andExpect(jsonPath("$.data.users[0].role").value("MEMBER"))
                .andExpect(jsonPath("$.data.users[0].roleName").value("Qora ishchi"))
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty());
    }

    @Test
    @DisplayName("createTaskV2: Workspace a'zosi bo'lmagan foydalanuvchi ID si uzatilganda 400 VALIDATION_ERROR")
    void createTaskV2_invalidUserId_returns400ValidationError() throws Exception {
        // 1. Mavjud bo'lmagan UUID
        String payloadNonExistent = """
                {
                  "columnId": "%s",
                  "title": "Vazifa",
                  "users": ["99999999-9999-9999-9999-999999999999"]
                }
                """.formatted(SEED_COL);

        mvc.perform(post("/api/v2/workspaces/" + SEED_WS + "/tasks")
                        .header("Authorization", bearer("xusan"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadNonExistent))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.data.users").isNotEmpty());

        // 2. Baza ichida bor, lekin boshqa workspace dagi foydalanuvchi
        String strangerName = "stranger_" + UUID.randomUUID().toString().substring(0, 6);
        User stranger = User.builder()
                .name(strangerName)
                .email(strangerName + "@example.com")
                .password(passwordEncoder.encode("password123"))
                .role(User.Role.USER)
                .build();
        userRepository.save(stranger);

        String payloadStranger = """
                {
                  "columnId": "%s",
                  "title": "Vazifa begona bilan",
                  "users": ["%s"]
                }
                """.formatted(SEED_COL, stranger.getId());

        mvc.perform(post("/api/v2/workspaces/" + SEED_WS + "/tasks")
                        .header("Authorization", bearer("xusan"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadStranger))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.data.users").value(containsString("Foydalanuvchi ushbu workspace a'zosi emas")));
    }

    @Test
    @DisplayName("createTaskV2: Noma'lum ustun ID si uzatilganda 404 NOT_FOUND")
    void createTaskV2_unknownColumn_returns404NotFound() throws Exception {
        String payload = """
                {
                  "columnId": "unknown-column-99999",
                  "title": "Ustuni yo'q vazifa"
                }
                """;

        mvc.perform(post("/api/v2/workspaces/" + SEED_WS + "/tasks")
                        .header("Authorization", bearer("xusan"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("createTaskV2: Sarlavha (title) berilmaganda 400 VALIDATION_ERROR")
    void createTaskV2_missingTitle_returns400ValidationError() throws Exception {
        String payloadEmptyTitle = """
                {
                  "columnId": "%s",
                  "title": ""
                }
                """.formatted(SEED_COL);

        mvc.perform(post("/api/v2/workspaces/" + SEED_WS + "/tasks")
                        .header("Authorization", bearer("xusan"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadEmptyTitle))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.data.title").isNotEmpty());
    }
}
