package com.taskcenter.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskcenter.dto.admin.*;
import com.taskcenter.model.User;
import com.taskcenter.service.AdminService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminService adminService;

    @Test
    @DisplayName("Admin dashboard stats - ROLE_ADMIN uchun 200 qaytadi")
    @WithMockUser(roles = "ADMIN")
    void getDashboardStats_asAdmin_returns200() throws Exception {
        AdminDashboardStatsDto stats = AdminDashboardStatsDto.builder()
                .totalUsers(120)
                .totalWorkspaces(15)
                .totalTasks(450)
                .completedTasks(300)
                .openTasks(150)
                .telegramLinkedUsers(80)
                .totalAttachments(25)
                .jvmUsedMemoryMb(120)
                .jvmMaxMemoryMb(512)
                .uptimeSeconds(3600)
                .botStatus("WEBHOOK_ACTIVE")
                .build();

        when(adminService.getDashboardStats()).thenReturn(stats);

        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalUsers").value(120))
                .andExpect(jsonPath("$.data.botStatus").value("WEBHOOK_ACTIVE"));
    }

    @Test
    @DisplayName("Admin dashboard stats - Oddiy ROLE_USER uchun 403 Forbidden beradi")
    @WithMockUser(roles = "USER")
    void getDashboardStats_asUser_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Foydalanuvchilar ro'yxati - sahifalash bilan 200 qaytadi")
    @WithMockUser(roles = "ADMIN")
    void getUsers_asAdmin_returnsList() throws Exception {
        AdminUserDto userDto = AdminUserDto.builder()
                .id("u-1")
                .name("testuser")
                .fullName("Test User")
                .email("test@example.com")
                .role("USER")
                .telegramChatId(123456789L)
                .telegramLinked(true)
                .workspaceCount(3)
                .build();

        when(adminService.getUsers(any(), any())).thenReturn(new PageImpl<>(List.of(userDto), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/admin/users?search=test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].name").value("testuser"))
                .andExpect(jsonPath("$.data.content[0].telegramLinked").value(true));
    }

    @Test
    @DisplayName("Foydalanuvchi rolini ADMIN ga o'zgartirish")
    @WithMockUser(roles = "ADMIN")
    void updateUserRole_asAdmin_updatesSuccessfully() throws Exception {
        AdminUserRoleRequest request = AdminUserRoleRequest.builder()
                .role(User.Role.ADMIN)
                .build();

        AdminUserDto updated = AdminUserDto.builder()
                .id("u-1")
                .name("testuser")
                .role("ADMIN")
                .build();

        when(adminService.updateUserRole(eq("u-1"), any())).thenReturn(updated);

        mockMvc.perform(put("/api/admin/users/u-1/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }

    @Test
    @DisplayName("Ish maydonlari ro'yxati - 200 OK")
    @WithMockUser(roles = "ADMIN")
    void getWorkspaces_asAdmin_returnsWorkspaces() throws Exception {
        AdminWorkspaceDto ws = AdminWorkspaceDto.builder()
                .id("ws-1")
                .name("Asosiy loyiha")
                .ownerName("Elshod")
                .memberCount(5)
                .taskCount(42)
                .createdAt(LocalDateTime.now())
                .build();

        when(adminService.getWorkspaces(any(), any())).thenReturn(new PageImpl<>(List.of(ws), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/admin/workspaces"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].name").value("Asosiy loyiha"))
                .andExpect(jsonPath("$.data.content[0].taskCount").value(42));
    }

    @Test
    @DisplayName("Telegram orqali ommaviy xabarnoma (Broadcast) yuborish")
    @WithMockUser(roles = "ADMIN")
    void broadcastTelegramMessage_asAdmin_returnsDeliveryStats() throws Exception {
        AdminBroadcastRequest request = AdminBroadcastRequest.builder()
                .message("Tizimda muhim yangilanish o'tkazildi!")
                .silent(false)
                .build();

        AdminBroadcastResultDto result = AdminBroadcastResultDto.builder()
                .totalRecipients(150)
                .successfulDeliveries(148)
                .failedDeliveries(2)
                .durationMs(240)
                .build();

        when(adminService.broadcastTelegramMessage(any())).thenReturn(result);

        mockMvc.perform(post("/api/admin/telegram/broadcast")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRecipients").value(150))
                .andExpect(jsonPath("$.data.successfulDeliveries").value(148));
    }

    @Test
    @DisplayName("Barcha keshlarni tozalash (Cache Evict)")
    @WithMockUser(roles = "ADMIN")
    void evictAllCaches_asAdmin_callsService() throws Exception {
        mockMvc.perform(post("/api/admin/cache/evict"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Barcha keshlar tozalandi"));

        verify(adminService).evictAllCaches();
    }
}
