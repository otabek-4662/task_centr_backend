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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WorkspaceMemberControllerTest {

    private static final String SEED_WS = "6a45163133ff7819b28ef909";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
    @DisplayName("A'zolar ro'yxatini rollari bilan olish")
    void getMembers_returnsMembersWithRoles() throws Exception {
        mvc.perform(get("/api/workspaces/" + SEED_WS + "/members")
                        .header("Authorization", bearer("xusan")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[?(@.name=='xusan')].role").value("OWNER"))
                .andExpect(jsonPath("$.data[?(@.name=='elshod')].role").value("MEMBER"));
    }

    @Test
    @DisplayName("Workspace egasi (Owner) yangi a'zo taklif qila oladi")
    void addMember_ownerCanAddMember() throws Exception {
        String randomName = "user_" + UUID.randomUUID().toString().substring(0, 6);
        User newUser = User.builder()
                .name(randomName)
                .email(randomName + "@test.local")
                .password(passwordEncoder.encode("password123"))
                .role(User.Role.USER)
                .build();
        userRepository.save(newUser);

        mvc.perform(post("/api/workspaces/" + SEED_WS + "/members")
                        .header("Authorization", bearer("xusan"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usernameOrEmail\":\"" + randomName + "\",\"role\":\"VIEWER\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value(randomName))
                .andExpect(jsonPath("$.data.role").value("VIEWER"));
    }

    @Test
    @DisplayName("Oddiy a'zo (Member) yangi a'zo taklif qila olmaydi (403 Forbidden)")
    void addMember_regularMemberCannotAdd() throws Exception {
        mvc.perform(post("/api/workspaces/" + SEED_WS + "/members")
                        .header("Authorization", bearer("elshod"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usernameOrEmail\":\"someuser\",\"role\":\"VIEWER\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A'zo rolini o'zgartirish (Owner muvaffaqiyatli o'zgartiradi)")
    void updateMemberRole_ownerCanUpdateRole() throws Exception {
        User elshod = userRepository.findByName("elshod").orElseThrow();

        mvc.perform(patch("/api/workspaces/" + SEED_WS + "/members/" + elshod.getId())
                        .header("Authorization", bearer("xusan"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }

    @Test
    @DisplayName("A'zoni jamoadan chiqarish (Owner a'zoni o'chira oladi)")
    void removeMember_ownerCanRemoveMember() throws Exception {
        User mirxon = userRepository.findByName("mirxon").orElseThrow();

        mvc.perform(delete("/api/workspaces/" + SEED_WS + "/members/" + mirxon.getId())
                        .header("Authorization", bearer("xusan")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
