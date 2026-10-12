package com.taskcenter.controller;

import com.taskcenter.base.BaseControllerIntegrationTest;
import com.taskcenter.dto.WorkspaceCreateRequest;
import com.taskcenter.fixture.UserFixture;
import com.taskcenter.fixture.WorkspaceFixture;
import com.taskcenter.model.User;
import com.taskcenter.model.Workspace;
import com.taskcenter.repository.WorkspaceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RealWorld (spring-boot-realworld-example-app) arxitekturasi va test uslubi
 * asosida qayta tashkil qilingan Workspace Controller integratsion testi.
 *
 * Farqlari:
 * 1. BaseControllerIntegrationTest orqali meros oladi — ortiqcha setup kodi yo'q.
 * 2. Login endpointiga HTTP so'rov yuborilmaydi, JWT bevosita JwtTokenProvider orqali olinadi.
 * 3. Qattiq kodlangan SEED_WS ga qaramlik yo'q; UserFixture va WorkspaceFixture orqali ma'lumotlar yaratiladi.
 * 4. JSON stringlarni concatenation qilish o'rniga DTO / Map va performGet/performPost ishlatiladi.
 */
@DisplayName("WorkspaceController — RealWorld Style Integration Tests")
class WorkspaceControllerRealWorldTest extends BaseControllerIntegrationTest {

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Test
    @DisplayName("Ro'yxat: Foydalanuvchi o'z workspace larini ko'ra oladi")
    void list_returnsWorkspacesForCurrentUser() throws Exception {
        // Arrange (Fixture orqali izolyatsiyalangan ma'lumot yaratish)
        User owner = userRepository.save(UserFixture.aUser().build());
        Workspace workspace = workspaceRepository.save(WorkspaceFixture.aWorkspace().ownerId(owner.getId()).build());

        // Act & Assert (Fluent RealWorld uslubidagi so'rov)
        performGet("/api/workspaces", bearer(owner.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[?(@.id=='" + workspace.getId() + "')]").exists());
    }

    @Test
    @DisplayName("Xavfsizlik: Tokensi so'rov yuborilganda 401 qaytaradi")
    void list_unauthenticated_returns401() throws Exception {
        performGet("/api/workspaces")
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Tafsilot: Mavjud workspace ma'lumotlarini ID orqali olish")
    void detail_returnsWorkspaceById() throws Exception {
        User owner = userRepository.save(UserFixture.aUser().build());
        Workspace workspace = workspaceRepository.save(
                WorkspaceFixture.aWorkspace()
                        .title("RealWorld Test Space")
                        .ownerId(owner.getId())
                        .build()
        );

        performGet("/api/workspaces/" + workspace.getId(), bearer(owner.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(workspace.getId()))
                .andExpect(jsonPath("$.data.title").value("RealWorld Test Space"))
                .andExpect(jsonPath("$.data.ownerId").value(owner.getId()));
    }

    @Test
    @DisplayName("CRUD Hayot sikli: Yaratish -> Yangilash -> O'chirish (Fluent)")
    void crud_lifecycle_fluent() throws Exception {
        User owner = userRepository.save(UserFixture.aUser().build());
        String token = bearer(owner.getName());
        String uniqueTitle = uniqueString("RW-Workspace");

        // 1. Create (POST)
        WorkspaceCreateRequest createReq = WorkspaceCreateRequest.builder()
                .title(uniqueTitle)
                .bgColor("#4f46e5")
                .description("RealWorld Architecture Demo")
                .build();

        MvcResult createResult = performPost("/api/workspaces", createReq, token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value(uniqueTitle))
                .andReturn();

        String workspaceId = extractDataId(createResult);

        // 2. Update (PUT)
        Map<String, Object> updateReq = Map.of(
                "title", uniqueTitle + "-Updated",
                "bgColor", "#10b981"
        );

        performPut("/api/workspaces/" + workspaceId, updateReq, token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value(uniqueTitle + "-Updated"))
                .andExpect(jsonPath("$.data.bgColor").value("#10b981"));

        // 3. Delete (DELETE)
        performDelete("/api/workspaces/" + workspaceId, token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 4. Verify Not Found (GET)
        performGet("/api/workspaces/" + workspaceId, token)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Validatsiya: Bo'sh nom bilan workspace yaratib bo'lmaydi (400 Bad Request)")
    void create_blankTitle_returns400() throws Exception {
        User owner = userRepository.save(UserFixture.aUser().build());
        WorkspaceCreateRequest invalidReq = WorkspaceCreateRequest.builder()
                .title("")
                .build();

        performPost("/api/workspaces", invalidReq, bearer(owner.getName()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
