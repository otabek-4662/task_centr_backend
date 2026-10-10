package com.taskcenter.dto;

import com.taskcenter.model.InvitationType;
import com.taskcenter.model.WorkspaceRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Ommaviy taklifnoma ko'rinishi (minimal xavfsiz ma'lumotlar)")
public class PublicInvitationDto {

    @Schema(description = "Ish maydoni (loyiha) nomi", example = "Dasturchilar Jamoasi")
    private String workspaceTitle;

    @Schema(description = "Taklif etuvchining ismi yoki username", example = "Otabek Sotimov")
    private String inviterName;

    @Schema(description = "Taklif etilayotgan rol: OWNER, ADMIN, MEMBER, VIEWER", example = "MEMBER")
    private WorkspaceRole role;

    @Schema(description = "Taklif turi: EMAIL yoki LINK", example = "LINK")
    private InvitationType type;

    @Schema(description = "Amal qilish muddati (ISO-8601 UTC, Z bilan). Havola muddatsiz bo'lsa null bo'ladi.", example = "2026-10-18T12:00:00Z", nullable = true)
    private Instant expiresAt;

    @Schema(description = "Havola muddati o'tgan yoki limit tugaganmi?", example = "false")
    private boolean expired;
}
