package com.taskcenter.controller;

import com.taskcenter.dto.ApiResponse;
import com.taskcenter.dto.FileUploadResponse;
import com.taskcenter.service.storage.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

@RestController
@RequestMapping("/api/files")
@Tag(name = "Fayllar (Files)", description = "Fayllarni yuklash va olish API lari")
public class FileController {

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            operationId = "uploadGeneralFile",
            summary = "Fayl yuklash (xabar biriktirmasi uchun)",
            description = "Chat yoki boshqa joylarda biriktirish uchun fayl yuklaydi va saqlangan fayl havolasi hamda thumbnail ma'lumotlarini qaytaradi."
    )
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadFile(
            @io.swagger.v3.oas.annotations.Parameter(description = "Yuklanadigan fayl (rasm, hujjat va h.k.)", required = true)
            @RequestParam("file") MultipartFile file) {
        
        FileUploadResponse response = fileStorageService.storeFileWithThumbnail(file, "chat-attachments");
        return ResponseEntity.ok(ApiResponse.success("Fayl muvaffaqiyatli yuklandi", response));
    }

    @GetMapping("/{fileName:.+}")
    @Operation(
            operationId = "downloadGeneralFile",
            summary = "Faylni yuklab olish yoki ko'rish",
            description = "Fayl nomi orqali saqlangan faylni to'g'ridan-to'g'ri ko'rish yoki yuklab olish imkonini beradi."
    )
    public ResponseEntity<Resource> downloadFile(
            @io.swagger.v3.oas.annotations.Parameter(description = "Yuklab olinadigan fayl nomi (kengaytmasi bilan)", example = "image123.png")
            @PathVariable String fileName, HttpServletRequest request) {
        Resource resource = fileStorageService.loadFileAsResource("chat-attachments/" + fileName);

        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            // Ignored
        }

        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
