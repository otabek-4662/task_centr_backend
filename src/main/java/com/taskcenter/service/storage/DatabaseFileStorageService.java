package com.taskcenter.service.storage;

import com.taskcenter.dto.FileUploadResponse;
import com.taskcenter.exception.BadRequestException;
import com.taskcenter.exception.ResourceNotFoundException;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "db")
public class DatabaseFileStorageService implements FileStorageService {

    private static final List<String> FORBIDDEN_EXTENSIONS = List.of("exe", "bat", "sh", "cmd", "php", "js", "html");
    private static final List<String> ALLOWED_EXTENSIONS = List.of(
            "jpg", "jpeg", "png", "gif", "webp", "svg", "pdf", "doc", "docx", "xls", "xlsx", "txt", "csv", "zip"
    );

    private final JdbcTemplate jdbcTemplate;

    public DatabaseFileStorageService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String storeFile(MultipartFile file, String subDirectory) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Yuklanayotgan fayl bo'sh bo'lishi mumkin emas");
        }

        String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "file"
        );
        if (originalFilename.contains("..")) {
            throw new BadRequestException("Fayl nomida noqonuniy belgilar mavjud: " + originalFilename);
        }

        validateExtension(originalFilename);

        String cleanSub = (subDirectory != null && !subDirectory.isBlank())
                ? subDirectory.trim().replaceAll("^/+|/+$", "")
                : "";
        String uniqueName = UUID.randomUUID() + "_" + originalFilename;
        String storagePath = cleanSub.isEmpty() ? uniqueName : (cleanSub + "/" + uniqueName);
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";

        try {
            byte[] bytes = file.getBytes();
            saveBytes(storagePath, contentType, bytes);
            return storagePath;
        } catch (IOException ex) {
            throw new RuntimeException("Faylni ma'lumotlar bazasiga saqlashda xatolik: " + originalFilename, ex);
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

        if (contentType.startsWith("image/") && !contentType.contains("svg")) {
            try {
                ByteArrayOutputStream thumbOut = new ByteArrayOutputStream();
                Thumbnails.of(new ByteArrayInputStream(file.getBytes()))
                        .size(200, 200)
                        .toOutputStream(thumbOut);
                byte[] thumbBytes = thumbOut.toByteArray();

                int slashIdx = storedPath.lastIndexOf('/');
                String thumbStoragePath = (slashIdx >= 0)
                        ? storedPath.substring(0, slashIdx + 1) + "thumb_" + storedPath.substring(slashIdx + 1)
                        : "thumb_" + storedPath;

                saveBytes(thumbStoragePath, contentType, thumbBytes);
                response.setThumbnailUrl("/api/files/" + thumbStoragePath);
            } catch (Exception ignored) {
            }
        }

        return response;
    }

    @Override
    public Resource loadFileAsResource(String storagePath) {
        if (storagePath == null || storagePath.isBlank() || storagePath.contains("..")) {
            throw new BadRequestException("Noqonuniy fayl yo'li: " + storagePath);
        }

        List<byte[]> rows = jdbcTemplate.query(
                "SELECT file_data FROM stored_files WHERE storage_path = ?",
                (rs, rowNum) -> rs.getBytes("file_data"),
                storagePath
        );

        if (rows.isEmpty() || rows.get(0) == null) {
            throw new ResourceNotFoundException("Fayl topilmadi: " + storagePath);
        }

        final String filename = storagePath.contains("/")
                ? storagePath.substring(storagePath.lastIndexOf('/') + 1)
                : storagePath;

        return new ByteArrayResource(rows.get(0)) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
    }

    @Override
    public void deleteFile(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            return;
        }
        jdbcTemplate.update("DELETE FROM stored_files WHERE storage_path = ?", storagePath);
    }

    private void saveBytes(String storagePath, String contentType, byte[] data) {
        jdbcTemplate.update(
                "INSERT INTO stored_files (storage_path, content_type, file_size, file_data) VALUES (?, ?, ?, ?) " +
                "ON CONFLICT (storage_path) DO UPDATE SET content_type = EXCLUDED.content_type, file_size = EXCLUDED.file_size, file_data = EXCLUDED.file_data",
                storagePath, contentType, (long) data.length, data
        );
    }

    private void validateExtension(String filename) {
        String ext = StringUtils.getFilenameExtension(filename);
        if (ext != null) {
            String lower = ext.toLowerCase();
            if (FORBIDDEN_EXTENSIONS.contains(lower)) {
                throw new BadRequestException("Bunday turdagi fayllarni yuklash taqiqlangan!");
            }
            if (!ALLOWED_EXTENSIONS.contains(lower)) {
                throw new BadRequestException("Faqat ruxsat etilgan rasm yoki hujjat formatlari qabul qilinadi!");
            }
        }
    }
}
