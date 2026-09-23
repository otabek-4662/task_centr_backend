package com.taskcenter.controller;

import com.taskcenter.dto.ApiResponse;
import com.taskcenter.dto.AttachmentDto;
import com.taskcenter.model.Attachment;
import com.taskcenter.model.User;
import com.taskcenter.service.AttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Tag(name = "Attachments", description = "Taskka fayl va rasm biriktirish API lari")
@SecurityRequirement(name = "bearerAuth")
@RestController
@CrossOrigin(origins = "*")
public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @Operation(summary = "Taskka fayl yoki rasm yuklash")
    @PostMapping(value = "/api/tasks/{taskId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AttachmentDto> uploadAttachment(
            @PathVariable String taskId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User currentUser) {
        AttachmentDto dto = attachmentService.uploadAttachment(taskId, file, currentUser);
        return ApiResponse.success("Fayl muvaffaqiyatli yuklandi", dto);
    }

    @Operation(summary = "Taskka biriktirilgan barcha fayllar ro'yxatini olish")
    @GetMapping("/api/tasks/{taskId}/attachments")
    public ApiResponse<List<AttachmentDto>> getAttachments(
            @PathVariable String taskId,
            @AuthenticationPrincipal User currentUser) {
        List<AttachmentDto> list = attachmentService.getAttachments(taskId, currentUser);
        return ApiResponse.success("ok", list);
    }

    @Operation(summary = "Faylni yuklab olish yoki ko'rish")
    @GetMapping("/api/attachments/{attachmentId}/download")
    public ResponseEntity<Resource> downloadAttachment(
            @PathVariable String attachmentId,
            @AuthenticationPrincipal User currentUser) {
        Attachment attachment = attachmentService.getAttachment(attachmentId, currentUser);
        Resource resource = attachmentService.loadFileAsResource(attachment);

        String contentType = attachment.getFileType();
        if (contentType == null || contentType.isBlank()) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        String encodedFileName = URLEncoder.encode(attachment.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName)
                .body(resource);
    }

    @Operation(summary = "Biriktirilgan faylni o'chirish")
    @DeleteMapping("/api/attachments/{attachmentId}")
    public ApiResponse<Void> deleteAttachment(
            @PathVariable String attachmentId,
            @AuthenticationPrincipal User currentUser) {
        attachmentService.deleteAttachment(attachmentId, currentUser);
        return ApiResponse.success("Fayl muvaffaqiyatli o'chirildi", null);
    }
}
