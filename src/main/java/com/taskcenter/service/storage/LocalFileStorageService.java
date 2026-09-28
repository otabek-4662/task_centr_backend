package com.taskcenter.service.storage;

import com.taskcenter.exception.BadRequestException;
import com.taskcenter.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.UUID;
import net.coobird.thumbnailator.Thumbnails;
import com.taskcenter.dto.FileUploadResponse;

@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    private final Path fileStorageLocation;

    public LocalFileStorageService(@Value("${app.upload.dir:./uploads}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException ex) {
            throw new RuntimeException("Yuklangan fayllar uchun papka yaratib bo'lmadi", ex);
        }
    }

    @Override
    public String storeFile(MultipartFile file, String subDirectory) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Yuklanayotgan fayl bo'sh bo'lishi mumkin emas");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
        if (originalFilename.contains("..")) {
            throw new BadRequestException("Fayl nomida noqonuniy belgilar mavjud: " + originalFilename);
        }

        String extension = StringUtils.getFilenameExtension(originalFilename);
        if (extension != null) {
            extension = extension.toLowerCase();
            java.util.List<String> forbidden = java.util.List.of("exe", "bat", "sh", "cmd", "php", "js", "html");
            java.util.List<String> allowed = java.util.List.of("jpg", "jpeg", "png", "gif", "webp", "svg", "pdf", "doc", "docx", "xls", "xlsx", "txt", "csv", "zip");
            
            if (forbidden.contains(extension)) {
                throw new BadRequestException("Bunday turdagi fayllarni yuklash taqiqlangan!");
            }
            if (!allowed.contains(extension)) {
                throw new BadRequestException("Faqat ruxsat etilgan rasm yoki hujjat formatlari qabul qilinadi!");
            }
        }

        try {
            Path targetDir = subDirectory != null && !subDirectory.isBlank()
                    ? this.fileStorageLocation.resolve(subDirectory).normalize()
                    : this.fileStorageLocation;
            Files.createDirectories(targetDir);

            String uniqueFileName = UUID.randomUUID().toString() + "_" + originalFilename;
            Path targetLocation = targetDir.resolve(uniqueFileName);

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return this.fileStorageLocation.relativize(targetLocation).toString().replace("\\", "/");
        } catch (IOException ex) {
            throw new RuntimeException("Faylni saqlashda xatolik yuz berdi: " + originalFilename, ex);
        }
    }

    @Override
    public FileUploadResponse storeFileWithThumbnail(MultipartFile file, String subDirectory) {
        String storedPath = storeFile(file, subDirectory);
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        String fileUrl = "/api/files/" + storedPath;
        
        FileUploadResponse response = FileUploadResponse.builder()
                .fileUrl(fileUrl)
                .fileType(contentType)
                .fileSize(file.getSize())
                .build();

        if (contentType.startsWith("image/")) {
            try {
                Path originalFilePath = this.fileStorageLocation.resolve(storedPath).normalize();
                String thumbFileName = "thumb_" + originalFilePath.getFileName().toString();
                Path thumbFilePath = originalFilePath.getParent().resolve(thumbFileName);
                
                Thumbnails.of(originalFilePath.toFile())
                        .size(200, 200)
                        .toFile(thumbFilePath.toFile());
                        
                String thumbStoredPath = this.fileStorageLocation.relativize(thumbFilePath).toString().replace("\\", "/");
                response.setThumbnailUrl("/api/files/" + thumbStoredPath);
            } catch (Exception e) {
                // Ignore thumbnail generation errors
            }
        }
        
        return response;
    }

    @Override
    public Resource loadFileAsResource(String storagePath) {
        try {
            Path filePath = this.fileStorageLocation.resolve(storagePath).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("Fayl topilmadi: " + storagePath);
            }
        } catch (MalformedURLException ex) {
            throw new ResourceNotFoundException("Fayl topilmadi: " + storagePath);
        }
    }

    @Override
    public void deleteFile(String storagePath) {
        try {
            Path filePath = this.fileStorageLocation.resolve(storagePath).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException ignored) {
        }
    }
}
