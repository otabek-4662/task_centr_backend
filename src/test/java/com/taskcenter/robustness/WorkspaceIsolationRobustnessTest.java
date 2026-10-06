package com.taskcenter.robustness;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class WorkspaceIsolationRobustnessTest {

    @Autowired
    private MockMvc mvc;

    private String createTestUser(String username) throws Exception {
        String body = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + username + "\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.data.token");
    }

    private String createWorkspace(String token, String title) throws Exception {
        String body = mvc.perform(post("/api/workspaces")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\",\"bgColor\":\"#000000\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.data.id");
    }

    private String getFirstColumn(String token, String wsId) throws Exception {
        String body = mvc.perform(get("/api/workspaces/" + wsId + "/columns")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.data[0].id");
    }

    private String createTask(String token, String wsId, String colId, String title) throws Exception {
        String body = mvc.perform(post("/api/workspaces/" + wsId + "/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"columnId\":\"" + colId + "\",\"title\":\"" + title + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.data.id");
    }

    @Test
    void userCannotReadTaskFromOtherWorkspace() throws Exception {
        String tokenX = createTestUser("user_wx_1");
        String wsX = createWorkspace(tokenX, "Workspace X");
        
        String tokenY = createTestUser("user_wy_1");
        String wsY = createWorkspace(tokenY, "Workspace Y");
        String colY = getFirstColumn(tokenY, wsY);
        String taskY = createTask(tokenY, wsY, colY, "Task in Y");

        // user_x tries to read taskY
        mvc.perform(get("/api/tasks/" + taskY).header("Authorization", "Bearer " + tokenX))
                .andExpect(status().isNotFound()); // or isForbidden
    }

    @Test
    void userCannotEditTaskFromOtherWorkspaceEvenWithFakeUrl() throws Exception {
        String tokenX = createTestUser("user_wx_2");
        String wsX = createWorkspace(tokenX, "Workspace X");
        
        String tokenY = createTestUser("user_wy_2");
        String wsY = createWorkspace(tokenY, "Workspace Y");
        String colY = getFirstColumn(tokenY, wsY);
        String taskY = createTask(tokenY, wsY, colY, "Task Y2");

        // user_x tries to update taskY by using wsX in the URL but taskY ID
        mvc.perform(put("/api/workspaces/" + wsX + "/tasks/" + taskY)
                        .header("Authorization", "Bearer " + tokenX)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Hacked Title\",\"columnId\":\"" + colY + "\"}"))
                .andExpect(status().isNotFound()); // or isForbidden
    }

    @Test
    void userCannotChangeColumnFromOtherWorkspace() throws Exception {
        String tokenX = createTestUser("user_wx_3");
        String wsX = createWorkspace(tokenX, "Workspace X");
        
        String tokenY = createTestUser("user_wy_3");
        String wsY = createWorkspace(tokenY, "Workspace Y");
        String colY = getFirstColumn(tokenY, wsY);

        mvc.perform(put("/api/workspaces/" + wsX + "/columns/" + colY)
                        .header("Authorization", "Bearer " + tokenX)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Hacked Col\"}"))
                .andExpect(status().isNotFound()); // or isForbidden
    }

    @Test
    void plainMemberCannotDeleteColumn() throws Exception {
        String ownerToken = createTestUser("user_owner_1");
        String wsId = createWorkspace(ownerToken, "Admin Workspace");
        String colId = getFirstColumn(ownerToken, wsId);

        String memberToken = createTestUser("user_member_1");
        // owner invites member
        mvc.perform(post("/api/workspaces/" + wsId + "/members")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"user_member_1\",\"role\":\"MEMBER\"}"))
                .andExpect(status().isCreated());

        // member tries to delete column
        mvc.perform(delete("/api/workspaces/" + wsId + "/columns/" + colId)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void removedUserLosesAccess() throws Exception {
        String ownerToken = createTestUser("user_owner_2");
        String wsId = createWorkspace(ownerToken, "Removal Workspace");
        String colId = getFirstColumn(ownerToken, wsId);
        String taskId = createTask(ownerToken, wsId, colId, "Secret Task");

        String memberToken = createTestUser("user_member_2");
        mvc.perform(post("/api/workspaces/" + wsId + "/members")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"user_member_2\",\"role\":\"MEMBER\"}"))
                .andExpect(status().isCreated());

        // can access
        mvc.perform(get("/api/tasks/" + taskId)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk());

        // remove member (need member's ID)
        String userJson = mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + memberToken))
                .andReturn().getResponse().getContentAsString();
        String memberId = JsonPath.read(userJson, "$.data.id");

        mvc.perform(delete("/api/workspaces/" + wsId + "/members/" + memberId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        // can no longer access
        mvc.perform(get("/api/tasks/" + taskId)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isNotFound()); // or isForbidden
    }
}
