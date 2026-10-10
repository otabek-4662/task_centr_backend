package com.taskcenter.dto;

import com.taskcenter.model.InvitationStatus;
import com.taskcenter.model.WorkspaceInvitation;
import com.taskcenter.model.WorkspaceRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.ZoneOffset;

@Data
@Builder
public class WorkspaceInvitationDto {
    @Schema(description = "Taklifnoma identifikatori (UUID)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6", format = "uuid")
    private String id;

    @Schema(description = "Taklif qilingan workspace identifikatori (UUID)", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7", format = "uuid")
    private String workspaceId;

    @Schema(description = "Taklif yuborgan foydalanuvchi identifikatori (UUID)", example = "d290f1ee-6c54-4b01-90e6-d701748f0851", format = "uuid")
    private String senderId;

    @Schema(description = "Taklif qilingan foydalanuvchi identifikatori (UUID). LINK turidagi takliflarda yoki tizimda hali mavjud bo'lmagan yangi email foydalanuvchilarida null bo'ladi. Taklif qabul qilingach to'ldiriladi.", example = "8a2f4c6e-1d3b-4c5a-9e7f-0b2a4c6e8d1a", format = "uuid", nullable = true)
    private String receiverId;

    @Schema(description = "Taklif qilingan foydalanuvchi elektron pochtasi. LINK turidagi umumiy takliflarda null bo'ladi.", example = "hamkasb@example.com", nullable = true)
    private String receiverEmail;

    @Schema(description = "Taklif qilingan rol: OWNER (asoschi), ADMIN (administrator), MEMBER (a'zo), VIEWER (kuzatuvchi)", example = "MEMBER")
    private WorkspaceRole role;

    @Schema(description = "Taklif holati: PENDING (kutilmoqda), ACCEPTED (qabul qilingan), REJECTED (rad etilgan), CANCELLED (bekor qilingan), EXPIRED (muddati o'tgan)", example = "PENDING")
    private InvitationStatus status;

    @Schema(description = "Yaratilgan vaqti (ISO-8601 UTC, Z bilan)", example = "2026-10-10T12:00:00Z")
    private Instant createdAt;

    @Schema(description = "Amal qilish muddati (ISO-8601 UTC, Z bilan). Agar havola muddatsiz bo'lsa null bo'ladi.", example = "2026-10-17T12:00:00Z", nullable = true)
    private Instant expiresAt;

    @Schema(description = "Workspace (ish maydoni) nomi", example = "Dasturchilar Jamoasi")
    private String workspaceTitle;

    @Schema(description = "Taklif qiluvchining to'liq ismi yoki username", example = "Otabek Sotimov")
    private String senderName;

    @Schema(description = "Taklif turi: EMAIL (aniq pochtaga yuborilgan) yoki LINK (umumiy havola orqali)", example = "EMAIL")
    private com.taskcenter.model.InvitationType type;

    @Schema(description = "Xom taklif tokeni (kamida 32 bayt URL-safe). Faqat taklif yangi yaratilganda yoki regenerate qilinganda qaytariladi; ro'yxatlarda yoki qayta o'qilganda xavfsizlik uchun null bo'ladi.", example = "pP3aK_91xL-w7Q3zD5eFg8hIjKlMnOpQrStUvWxYz01", nullable = true)
    private String token;

    @Schema(description = "Havoladan foydalanish maksimal soni (faqat LINK uchun). EMAIL turida 1; cheksiz LINK larda null bo'ladi.", example = "10", nullable = true)
    private Integer maxUses;

    @Schema(description = "Havoladan amalda muvaffaqiyatli foydalanilganlar soni", example = "3")
    private Integer useCount;

    @Schema(description = "Email yetkazish holati: PENDING (navbatda), SENT (yetkazildi), FAILED (xatolik), SKIPPED (o'tkazildi), NOT_APPLICABLE (LINK turida yoki Telegram botda)", example = "SENT")
    private com.taskcenter.model.EmailDeliveryStatus emailDeliveryStatus;

    @Schema(description = "Email jo'natish xatosi (muvaffaqiyatsiz bo'lsa). Xato bo'lmaganda null bo'ladi.", example = "HTTP 400: Domain not verified", nullable = true)
    private String emailDeliveryError;

    @Schema(description = "Telegram Bot taklif havolasi. Faqat token qaytarilganda to'ldiriladi, aks holda null bo'ladi.", example = "https://t.me/task_center_bot?start=inv_pP3aK_91xL-w7Q3zD5eFg8hIjKlMnOpQrStUvWxYz01", nullable = true)
    private String telegramInviteLink;

    @Schema(description = "Telegram Mini App taklif havolasi. Faqat token qaytarilganda to'ldiriladi, aks holda null bo'ladi.", example = "https://t.me/task_center_bot/app?startapp=inv_pP3aK_91xL-w7Q3zD5eFg8hIjKlMnOpQrStUvWxYz01", nullable = true)
    private String telegramMiniappLink;

    @Schema(description = "Web ilova taklif havolasi. Faqat token qaytarilganda to'ldiriladi, aks holda null bo'ladi.", example = "https://task-center-frontend.onrender.com/invite/pP3aK_91xL-w7Q3zD5eFg8hIjKlMnOpQrStUvWxYz01", nullable = true)
    private String inviteLink;

    public static WorkspaceInvitationDto fromEntity(WorkspaceInvitation entity) {
        if (entity == null) return null;
        return WorkspaceInvitationDto.builder()
                .id(entity.getId())
                .workspaceId(entity.getWorkspaceId())
                .senderId(entity.getSenderId())
                .receiverId(entity.getReceiverId())
                .receiverEmail(entity.getReceiverEmail())
                .type(entity.getType())
                .maxUses(entity.getMaxUses())
                .useCount(entity.getUseCount())
                .emailDeliveryStatus(entity.getEmailDeliveryStatus())
                .emailDeliveryError(entity.getEmailDeliveryError())
                .role(entity.getRole())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toInstant(ZoneOffset.UTC) : null)
                .expiresAt(entity.getExpiresAt() != null ? entity.getExpiresAt().toInstant(ZoneOffset.UTC) : null)
                .build();
    }
}
