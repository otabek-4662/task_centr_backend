package com.taskcenter.controller;

import com.taskcenter.dto.*;
import com.taskcenter.model.User;
import com.taskcenter.model.WorkspaceRole;
import com.taskcenter.service.EmailService;
import com.taskcenter.service.WorkspaceInvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Workspace Invitations", description = "Workspace ga taklif qilish API lari (Trello/Jira mantiqida)")
@SecurityRequirement(name = "bearerAuth")
public class WorkspaceInvitationController {

    private final WorkspaceInvitationService invitationService;
    private final EmailService emailService;

    public WorkspaceInvitationController(WorkspaceInvitationService invitationService,
                                         EmailService emailService) {
        this.invitationService = invitationService;
        this.emailService = emailService;
    }

    @Operation(operationId = "createWorkspaceEmailInvitation", summary = "Email orqali taklif yuborish (Admin/Owner)",
            description = "Foydalanuvchining aniq emailiga rasmiy taklifnoma yuboradi. Faqat OWNER yoki ADMIN bajara oladi.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Taklif muvaffaqiyatli yuborildi"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validatsiya xatosi yoki limit (VALIDATION_ERROR, CANNOT_INVITE_OWNER, INVITE_LIMIT_REACHED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Autentifikatsiyadan o'tilmagan"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Amalni bajarishga ruxsat yo'q (FORBIDDEN_ROLE)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Ish maydoni topilmadi"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Foydalanuvchi allaqachon a'zo yoki taklif mavjud (ALREADY_MEMBER, DUPLICATE_PENDING_INVITE)")
    })
    @PostMapping("/workspaces/{workspaceId}/invites")
    public ApiResponse<WorkspaceInvitationDto> inviteUser(
            @Parameter(description = "Ishchi maydon identifikatori (UUID)", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7", required = true)
            @PathVariable String workspaceId,
            @Valid @RequestBody InviteRequestDto request,
            @AuthenticationPrincipal User currentUser) {
        WorkspaceInvitationDto dto = invitationService.inviteUser(workspaceId, request, currentUser);
        return ApiResponse.success("Taklif muvaffaqiyatli yuborildi", dto);
    }

    @Operation(operationId = "createWorkspaceLinkInvitation", summary = "Havola orqali taklif yaratish (Admin/Owner)",
            description = "Email ko'rsatilmagan ochiq havola (invite link) yaratadi. Rol, muddat va max foydalanish soni belgilanadi.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Taklif havolasi muvaffaqiyatli yaratildi"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validatsiya xatosi yoki limit (VALIDATION_ERROR, CANNOT_INVITE_OWNER, INVITE_LIMIT_REACHED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Autentifikatsiyadan o'tilmagan"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Amalni bajarishga ruxsat yo'q (FORBIDDEN_ROLE)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Ish maydoni topilmadi")
    })
    @PostMapping("/workspaces/{workspaceId}/invites/link")
    public ApiResponse<WorkspaceInvitationDto> createLinkInvite(
            @Parameter(description = "Ishchi maydon identifikatori (UUID)", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7", required = true)
            @PathVariable String workspaceId,
            @Valid @RequestBody(required = false) CreateLinkInviteRequest request,
            @AuthenticationPrincipal User currentUser) {
        WorkspaceInvitationDto dto = invitationService.createOrGetLinkInvite(workspaceId, request, currentUser);
        return ApiResponse.success("Taklif havolasi muvaffaqiyatli yaratildi", dto);
    }

    @Operation(operationId = "regenerateWorkspaceLinkInvitation", summary = "Taklif havolasini yangilash (Admin/Owner)",
            description = "Eski faol taklif havolasini bekor qilib, yangi xavfsiz token bilan havola yaratadi.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Yangi taklif havolasi generatsiya qilindi"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validatsiya xatosi (VALIDATION_ERROR, CANNOT_INVITE_OWNER)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Autentifikatsiyadan o'tilmagan"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Amalni bajarishga ruxsat yo'q (FORBIDDEN_ROLE)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Ish maydoni topilmadi")
    })
    @PostMapping("/workspaces/{workspaceId}/invites/link/regenerate")
    public ApiResponse<WorkspaceInvitationDto> regenerateLinkInvite(
            @Parameter(description = "Ishchi maydon identifikatori (UUID)", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7", required = true)
            @PathVariable String workspaceId,
            @Valid @RequestBody(required = false) CreateLinkInviteRequest request,
            @AuthenticationPrincipal User currentUser) {
        WorkspaceInvitationDto dto = invitationService.regenerateLinkInvite(workspaceId, request, currentUser);
        return ApiResponse.success("Yangi taklif havolasi generatsiya qilindi", dto);
    }

    @Operation(operationId = "resendWorkspaceEmailInvitation", summary = "Email taklifnomani qayta yuborish (Admin/Owner)",
            description = "Kutilayotgan email taklifnomani Brevo orqali qayta yuboradi.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Taklifnoma qayta yuborildi"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Noto'g'ri so'rov (VALIDATION_ERROR, INVITE_ALREADY_USED, INVALID_INVITE_TYPE)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Autentifikatsiyadan o'tilmagan"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Amalni bajarishga ruxsat yo'q (FORBIDDEN_ROLE)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Taklif topilmadi (INVITE_NOT_FOUND)")
    })
    @PostMapping("/workspaces/{workspaceId}/invites/{id}/resend")
    public ApiResponse<WorkspaceInvitationDto> resendEmailInvite(
            @Parameter(description = "Ishchi maydon identifikatori (UUID)", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7", required = true)
            @PathVariable String workspaceId,
            @Parameter(description = "Taklifnoma identifikatori (UUID)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6", required = true)
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        WorkspaceInvitationDto dto = invitationService.resendEmailInvite(workspaceId, id, currentUser);
        return ApiResponse.success("Taklifnoma qayta yuborildi", dto);
    }

    @Operation(operationId = "createTelegramInviteLink", summary = "Telegram taklif havolasini yaratish (Admin/Owner)",
            description = "Workspace uchun Telegram orqali ulashish mumkin bo'lgan taklif havolasini generatsiya qiladi.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Telegram taklif havolasi yaratildi"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Noto'g'ri so'rov (CANNOT_INVITE_OWNER, INVITE_LIMIT_REACHED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Autentifikatsiyadan o'tilmagan"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Amalni bajarishga ruxsat yo'q (FORBIDDEN_ROLE)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Ish maydoni topilmadi")
    })
    @PostMapping("/workspaces/{workspaceId}/invites/telegram-link")
    public ApiResponse<WorkspaceInvitationDto> createTelegramInvite(
            @Parameter(description = "Ishchi maydon identifikatori (UUID)", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7", required = true)
            @PathVariable String workspaceId,
            @RequestParam(required = false, defaultValue = "MEMBER") WorkspaceRole role,
            @AuthenticationPrincipal User currentUser) {
        WorkspaceInvitationDto dto = invitationService.createTelegramInvite(workspaceId, role, currentUser);
        return ApiResponse.success("Telegram taklif havolasi yaratildi", dto);
    }

    @Operation(operationId = "testBrevoEmailSending", summary = "Brevo email yuborilishini tekshirish (Admin/Owner)",
            description = "Brevo API orqali so'rov yuboruvchining o'z manziliga sinov xatini yuboradi (API kaliti oshkor qilinmaydi, soatiga 5 ta limit).")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Brevo sinov natijasi"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Telegram profilida email yo'q (INVITE_DUMMY_EMAIL)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Autentifikatsiyadan o'tilmagan"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Amalni bajarishga ruxsat yo'q (FORBIDDEN_ROLE)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Ish maydoni topilmadi"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Soatlik sinov limiti oshdi (TEST_EMAIL_LIMIT)")
    })
    @PostMapping("/workspaces/{workspaceId}/invites/test-email")
    public ApiResponse<TestEmailResponse> testEmailSending(
            @Parameter(description = "Ishchi maydon identifikatori (UUID)", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7", required = true)
            @PathVariable String workspaceId,
            @RequestBody(required = false) TestEmailRequest request,
            @AuthenticationPrincipal User currentUser) {
        TestEmailResponse response = invitationService.testEmail(workspaceId, currentUser);
        return ApiResponse.success("Brevo sinov natijasi", response);
    }

    @Operation(operationId = "getWorkspaceInvitations", summary = "Workspace takliflarini ko'rish (Admin/Owner)",
            description = "Berilgan workspace ga tegishli barcha taklifnomalar (email yetkazilish holati, limitlar va ro'yxat)ni ko'rish.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Taklifnomalar ro'yxati"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Autentifikatsiyadan o'tilmagan"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Amalni bajarishga ruxsat yo'q (FORBIDDEN_ROLE)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Ish maydoni topilmadi")
    })
    @GetMapping("/workspaces/{workspaceId}/invites")
    public ApiResponse<List<WorkspaceInvitationDto>> getWorkspaceInvitations(
            @Parameter(description = "Ishchi maydon identifikatori (UUID)", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7", required = true)
            @PathVariable String workspaceId,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.success("ok", invitationService.getWorkspaceInvitations(workspaceId, currentUser));
    }

    @Operation(operationId = "cancelWorkspaceInvitation", summary = "Taklifni bekor qilish (Admin/Owner)",
            description = "Yuborilgan taklifnomani bekor qiladi. Faqat OWNER yoki ADMIN bajara oladi.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Taklif muvaffaqiyatli bekor qilindi"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Noto'g'ri so'rov (VALIDATION_ERROR)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Autentifikatsiyadan o'tilmagan"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Amalni bajarishga ruxsat yo'q (FORBIDDEN_ROLE)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Taklif topilmadi (INVITE_NOT_FOUND)")
    })
    @DeleteMapping("/workspaces/{workspaceId}/invites/{id}")
    public ApiResponse<Void> cancelInvitation(
            @Parameter(description = "Ishchi maydon identifikatori (UUID)", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7", required = true)
            @PathVariable String workspaceId,
            @Parameter(description = "Taklifnoma identifikatori (UUID)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6", required = true)
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        invitationService.cancelInvitation(workspaceId, id, currentUser);
        return ApiResponse.success("Taklif bekor qilindi", null);
    }

    @Operation(operationId = "getMyPendingInvitations", summary = "Menga kelgan faol takliflarni ko'rish",
            description = "Joriy kirgan foydalanuvchining hisobiga kelgan barcha faol taklifnomalarni qaytaradi.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Foydalanuvchiga yuborilgan taklifnomalar ro'yxati"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Autentifikatsiyadan o'tilmagan")
    })
    @GetMapping("/invitations/me")
    public ApiResponse<List<WorkspaceInvitationDto>> getMyInvitations(
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.success("ok", invitationService.getMyPendingInvitations(currentUser));
    }

    @Operation(operationId = "getPublicInvitationPreview", summary = "Ommaviy taklifnoma ma'lumotlarini olish (Minimal/Xavfsiz)",
            description = "Token orqali taklifnoma sahifasini ochish uchun minimal ma'lumot (ish maydoni nomi, taklif etuvchi, rol). Avtorizatsiya talab qilmaydi, rate-limited.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Ommaviy taklif ma'lumotlari"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Taklif topilmadi yoki muddati o'tgan (INVITE_NOT_FOUND, INVITE_CANCELLED, INVITE_EXPIRED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "So'rovlar soni oshdi (RATE_LIMITED)")
    })
    @GetMapping("/invitations/{token}")
    public ApiResponse<PublicInvitationDto> getPublicInvitation(
            @Parameter(description = "URL-safe taklif tokeni (kamida 32 bayt)", example = "pP3aK_91xL-w7Q3zD5eFg8hIjKlMnOpQrStUvWxYz01", required = true)
            @PathVariable String token) {
        return ApiResponse.success("ok", invitationService.getPublicInvitation(token));
    }

    @Operation(operationId = "getInvitationDetails", summary = "Taklifnoma to'liq ma'lumotlarini olish (Ichki/Batafsil)",
            description = "Taklifnoma tokeni yoki ID si orqali to'liq ma'lumotlarni ko'rish.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Taklifnoma to'liq ma'lumotlari"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Autentifikatsiyadan o'tilmagan"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Taklif topilmadi (INVITE_NOT_FOUND)")
    })
    @GetMapping("/invitations/{id}/details")
    public ApiResponse<WorkspaceInvitationDto> getInvitationDetails(
            @Parameter(description = "Taklifnoma identifikatori (UUID) yoki tokeni", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6", required = true)
            @PathVariable String id) {
        return ApiResponse.success("ok", invitationService.getInvitationDetails(id));
    }

    @Operation(operationId = "acceptWorkspaceInvitationByToken", summary = "Taklifni qabul qilish",
            description = "Taklifnoma tokeni orqali jamoaga qo'shilish (foydalanuvchi tizimga kirgan bo'lishi kerak).")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Taklif qabul qilindi va jamoaga qo'shildingiz"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Yaroqsiz taklif holati (INVITE_CANCELLED, INVITE_ALREADY_USED, INVITE_EXPIRED, INVITE_LIMIT_REACHED, INVITE_DUMMY_EMAIL)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Autentifikatsiyadan o'tilmagan"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Taklif boshqa foydalanuvchiga tegishli (INVITE_NOT_FOR_USER, INVITE_EMAIL_MISMATCH)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Taklif topilmadi (INVITE_NOT_FOUND)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Allaqachon ushbu ish maydoni a'zosisiz (ALREADY_MEMBER)")
    })
    @PostMapping("/invitations/{token}/accept")
    public ApiResponse<Void> acceptInvitation(
            @Parameter(description = "URL-safe taklif tokeni yoki ID si", example = "pP3aK_91xL-w7Q3zD5eFg8hIjKlMnOpQrStUvWxYz01", required = true)
            @PathVariable String token,
            @AuthenticationPrincipal User currentUser) {
        invitationService.acceptInvitation(token, currentUser);
        return ApiResponse.success("Taklif qabul qilindi va jamoaga qo'shildingiz", null);
    }

    @Operation(operationId = "rejectWorkspaceInvitationByToken", summary = "Taklifni rad etish",
            description = "Kelgan taklifnomani rad etish.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Taklif rad etildi"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Taklif allaqachon yakunlangan (INVITE_ALREADY_USED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Autentifikatsiyadan o'tilmagan"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Taklif sizga tegishli emas (INVITE_NOT_FOR_USER, INVITE_EMAIL_MISMATCH)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Taklif topilmadi (INVITE_NOT_FOUND)")
    })
    @PostMapping("/invitations/{token}/reject")
    public ApiResponse<Void> rejectInvitation(
            @Parameter(description = "URL-safe taklif tokeni yoki ID si", example = "pP3aK_91xL-w7Q3zD5eFg8hIjKlMnOpQrStUvWxYz01", required = true)
            @PathVariable String token,
            @AuthenticationPrincipal User currentUser) {
        invitationService.rejectInvitation(token, currentUser);
        return ApiResponse.success("Taklif rad etildi", null);
    }

    @Operation(operationId = "acceptWorkspaceInvitationById", summary = "Taklifni ID bo'yicha qabul qilish (/me ro'yxatidan)",
            description = "Faqat taklif egasi (receiverId) o'ziga yuborilgan EMAIL taklifni ID bo'yicha qabul qilishi mumkin.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Taklif qabul qilindi va jamoaga qo'shildingiz"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Noto'g'ri taklif turi yoki holati (INVALID_INVITE_TYPE, INVITE_CANCELLED, INVITE_ALREADY_USED, INVITE_EXPIRED, INVITE_LIMIT_REACHED, INVITE_DUMMY_EMAIL)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Autentifikatsiyadan o'tilmagan"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Taklif joriy foydalanuvchiga tegishli emas (INVITE_NOT_FOR_USER)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Taklif topilmadi (INVITE_NOT_FOUND)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Allaqachon ish maydoni a'zosisiz (ALREADY_MEMBER)")
    })
    @PostMapping("/invitations/by-id/{id}/accept")
    public ApiResponse<Void> acceptInvitationById(
            @Parameter(description = "Taklifnoma identifikatori (UUID)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6", required = true)
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        invitationService.acceptInvitationById(id, currentUser);
        return ApiResponse.success("Taklif qabul qilindi va jamoaga qo'shildingiz", null);
    }

    @Operation(operationId = "rejectWorkspaceInvitationById", summary = "Taklifni ID bo'yicha rad etish (/me ro'yxatidan)",
            description = "Faqat taklif egasi (receiverId) o'ziga yuborilgan EMAIL taklifni ID bo'yicha rad qilishi mumkin.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Taklif rad etildi"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Noto'g'ri taklif turi yoki allaqachon yakunlangan (INVALID_INVITE_TYPE, INVITE_ALREADY_USED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Autentifikatsiyadan o'tilmagan"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Taklif joriy foydalanuvchiga tegishli emas (INVITE_NOT_FOR_USER)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Taklif topilmadi (INVITE_NOT_FOUND)")
    })
    @PostMapping("/invitations/by-id/{id}/reject")
    public ApiResponse<Void> rejectInvitationById(
            @Parameter(description = "Taklifnoma identifikatori (UUID)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6", required = true)
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        invitationService.rejectInvitationById(id, currentUser);
        return ApiResponse.success("Taklif rad etildi", null);
    }

    @Operation(operationId = "acceptWorkspaceInvitationLegacy", summary = "Taklifni qabul qilish (Legacy PATCH)",
            description = "Eski frontend mijozlari uchun PATCH metodi orqali qabul qilish.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Taklif qabul qilindi"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Yaroqsiz taklif holati"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Autentifikatsiyadan o'tilmagan"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Taklif topilmadi")
    })
    @PatchMapping("/invitations/{id}/accept")
    public ApiResponse<Void> acceptInvitationLegacy(
            @Parameter(description = "Taklifnoma identifikatori yoki tokeni", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6", required = true)
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        invitationService.acceptInvitation(id, currentUser);
        return ApiResponse.success("Taklif qabul qilindi va jamoaga qo'shildingiz", null);
    }

    @Operation(operationId = "rejectWorkspaceInvitationLegacy", summary = "Taklifni rad etish (Legacy PATCH)",
            description = "Eski frontend mijozlari uchun PATCH metodi orqali rad etish.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Taklif rad etildi"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Yaroqsiz taklif holati"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Autentifikatsiyadan o'tilmagan"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Taklif topilmadi")
    })
    @PatchMapping("/invitations/{id}/reject")
    public ApiResponse<Void> rejectInvitationLegacy(
            @Parameter(description = "Taklifnoma identifikatori yoki tokeni", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6", required = true)
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        invitationService.rejectInvitation(id, currentUser);
        return ApiResponse.success("Taklif rad etildi", null);
    }
}
