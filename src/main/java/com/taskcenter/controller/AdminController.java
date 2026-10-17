package com.taskcenter.controller;

import com.taskcenter.dto.ApiResponse;
import com.taskcenter.dto.admin.*;
import com.taskcenter.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Panel", description = "Platforma SuperAdmin boshqaruvi, metrikalar va monitoring API'lari")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dashboard")
    @Operation(summary = "Tizim ko'rsatkichlari (Dashboard)", description = "Foydalanuvchilar, vazifalar, ish maydonlari, JVM va bot telemetriyasini qaytaradi")
    public ResponseEntity<ApiResponse<AdminDashboardStatsDto>> getDashboardStats() {
        return ResponseEntity.ok(ApiResponse.success("Tizim ko'rsatkichlari", adminService.getDashboardStats()));
    }

    @GetMapping("/users")
    @Operation(summary = "Foydalanuvchilar ro'yxati", description = "Barcha foydalanuvchilarni sahifalash va qidiruv bilan qaytaradi")
    public ResponseEntity<ApiResponse<Page<AdminUserDto>>> getUsers(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Foydalanuvchilar ro'yxati", adminService.getUsers(search, pageable)));
    }

    @PutMapping("/users/{userId}/role")
    @Operation(summary = "Foydalanuvchi rolini yangilash", description = "Foydalanuvchi rolini USER yoki ADMIN ga o'zgartiradi")
    public ResponseEntity<ApiResponse<AdminUserDto>> updateUserRole(
            @PathVariable String userId,
            @Valid @RequestBody AdminUserRoleRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Foydalanuvchi roli yangilandi", adminService.updateUserRole(userId, request)));
    }

    @DeleteMapping("/users/{userId}")
    @Operation(summary = "Foydalanuvchini o'chirish", description = "Foydalanuvchini soft-delete qiladi")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable String userId) {
        adminService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponse.success("Foydalanuvchi o'chirildi", null));
    }

    @GetMapping("/workspaces")
    @Operation(summary = "Ish maydonlari ro'yxati", description = "Barcha workspace'larni a'zolari va vazifalar soni bilan qaytaradi")
    public ResponseEntity<ApiResponse<Page<AdminWorkspaceDto>>> getWorkspaces(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Ish maydonlari ro'yxati", adminService.getWorkspaces(search, pageable)));
    }

    @DeleteMapping("/workspaces/{workspaceId}")
    @Operation(summary = "Ish maydonini o'chirish", description = "Ish maydonini soft-delete qiladi")
    public ResponseEntity<ApiResponse<Void>> deleteWorkspace(@PathVariable String workspaceId) {
        adminService.deleteWorkspace(workspaceId);
        return ResponseEntity.ok(ApiResponse.success("Ish maydoni o'chirildi", null));
    }

    @PostMapping("/telegram/broadcast")
    @Operation(summary = "Telegram orqali ommaviy xabarnoma (Broadcast)", description = "Barcha Telegram bot ulangan foydalanuvchilarga xabar yuboradi")
    public ResponseEntity<ApiResponse<AdminBroadcastResultDto>> broadcastTelegramMessage(
            @Valid @RequestBody AdminBroadcastRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Xabarnoma yuborildi", adminService.broadcastTelegramMessage(request)));
    }

    @GetMapping("/jobs")
    @Operation(summary = "Fon xizmatlari (Background Jobs)", description = "Rejalashtirilgan tozalash va fon xizmatlari ro'yxatini qaytaradi")
    public ResponseEntity<ApiResponse<List<AdminSystemJobDto>>> getSystemJobs() {
        return ResponseEntity.ok(ApiResponse.success("Fon xizmatlari", adminService.getSystemJobs()));
    }

    @PostMapping("/jobs/{jobKey}/trigger")
    @Operation(summary = "Fon xizmatini qo'lda ishga tushirish", description = "Tanlangan xizmatni darhol ishga tushiradi")
    public ResponseEntity<ApiResponse<Void>> triggerSystemJob(@PathVariable String jobKey) {
        adminService.triggerSystemJob(jobKey);
        return ResponseEntity.ok(ApiResponse.success("Xizmat ishga tushirildi", null));
    }

    @PostMapping("/cache/evict")
    @Operation(summary = "Barcha keshlarni tozalash", description = "Tizimdagi barcha Caffeine keshlarini tozalaydi")
    public ResponseEntity<ApiResponse<Void>> evictAllCaches() {
        adminService.evictAllCaches();
        return ResponseEntity.ok(ApiResponse.success("Barcha keshlar tozalandi", null));
    }
}
