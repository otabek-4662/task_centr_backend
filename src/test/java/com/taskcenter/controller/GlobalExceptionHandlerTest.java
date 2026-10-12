package com.taskcenter.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskcenter.model.User;
import com.taskcenter.model.Workspace;
import com.taskcenter.repository.UserRepository;
import com.taskcenter.repository.WorkspaceRepository;
import com.taskcenter.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private User currentUser;
    private String validToken;

    @BeforeEach
    void setUp() {
        String username = "errortestuser_" + UUID.randomUUID().toString().substring(0, 8);
        currentUser = userRepository.save(User.builder()
                .name(username)
                .password("hashpassword")
                .role(User.Role.USER)
                .build());
        validToken = tokenProvider.generateTokenFromUser(currentUser);
    }

    @Test
    @DisplayName("1. Tokensiz himoyalangan endpointga so'rov yuborilganda 401 va standart ApiError JSON qaytadi")
    void testUnauthorizedWithoutToken() throws Exception {
        mockMvc.perform(get("/api/workspaces"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/workspaces"));
    }

    @Test
    @DisplayName("2. Yaroqsiz token bilan so'rov yuborilganda 401 va standart ApiError JSON qaytadi")
    void testUnauthorizedWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/workspaces")
                        .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/workspaces"));
    }

    @Test
    @DisplayName("3. Noto'g'ri body (validatsiya xatosi) yuborilganda 400, error=BAD_REQUEST va fieldErrors qaytadi")
    void testValidationErrorReturnsFieldErrors() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/auth/login"))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.fieldErrors[0].field").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors[0].message").isNotEmpty());
    }

    @Test
    @DisplayName("4. Mavjud bo'lmagan resurs so'ralganda 404 va standart ApiError JSON qaytadi")
    void testNotFoundReturnsApiError() throws Exception {
        String nonExistentId = UUID.randomUUID().toString();
        mockMvc.perform(get("/api/workspaces/" + nonExistentId)
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/workspaces/" + nonExistentId));
    }

    @Test
    @DisplayName("5. Boshqa workspace resursiga so'rov yuborilganda 403 va standart ApiError JSON qaytadi")
    void testForbiddenWhenAccessingAnotherWorkspace() throws Exception {
        // Boshqa egaga tegishli workspace yaratamiz
        User otherOwner = userRepository.save(User.builder()
                .name("otherowner_" + UUID.randomUUID().toString().substring(0, 8))
                .password("hashpassword")
                .role(User.Role.USER)
                .build());

        Workspace otherWorkspace = workspaceRepository.save(Workspace.builder()
                .title("Other Workspace")
                .ownerId(otherOwner.getId())
                .keyPrefix("OTH-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase())
                .build());

        // currentUser ushbu workspace a'zosi emas -> 403 FORBIDDEN qaytishi shart
        mockMvc.perform(get("/api/workspaces/" + otherWorkspace.getId())
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/workspaces/" + otherWorkspace.getId()));
    }

    @Test
    @DisplayName("6. Qo'llab-quvvatlanmaydigan HTTP metodi yuborilganda 405 va standart ApiError JSON qaytadi")
    void testMethodNotSupportedReturns405() throws Exception {
        mockMvc.perform(post("/api/invitations/public-token-test")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.error").value("METHOD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/invitations/public-token-test"));
    }
}
