package com.taskcenter.controller;

import com.jayway.jsonpath.JsonPath;
import com.taskcenter.model.User;
import com.taskcenter.repository.UserRepository;
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
class ContactControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

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
    @DisplayName("GET /api/contacts: ro'yxatni muvaffaqiyatli qaytaradi")
    void getContacts_success() throws Exception {
        mvc.perform(get("/api/contacts")
                        .header("Authorization", bearer("elshod")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("POST /api/contacts: yangi kontakt qo'shish va DELETE orqali o'chirish")
    void addAndDeleteContact_flow() throws Exception {
        User xusan = userRepository.findByName("xusan").orElseThrow();

        // 1. Add contact
        mvc.perform(post("/api/contacts")
                        .header("Authorization", bearer("elshod"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usernameOrEmail\":\"xusan\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("xusan"))
                .andExpect(jsonPath("$.data.id").value(xusan.getId()));

        // 2. Duplicate add gives 409
        mvc.perform(post("/api/contacts")
                        .header("Authorization", bearer("elshod"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usernameOrEmail\":\"xusan\"}"))
                .andExpect(status().isConflict());

        // 3. Delete contact
        mvc.perform(delete("/api/contacts/" + xusan.getId())
                        .header("Authorization", bearer("elshod")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/contacts: o'zini qo'shishga uringanda 400 beradi")
    void addContact_self_returns400() throws Exception {
        mvc.perform(post("/api/contacts")
                        .header("Authorization", bearer("elshod"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usernameOrEmail\":\"elshod\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/contacts: topilmagan foydalanuvchida 404 beradi")
    void addContact_notFound_returns404() throws Exception {
        mvc.perform(post("/api/contacts")
                        .header("Authorization", bearer("elshod"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usernameOrEmail\":\"non_existing_user_999\"}"))
                .andExpect(status().isNotFound());
    }
}
