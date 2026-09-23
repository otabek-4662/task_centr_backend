package com.taskcenter.service;

import com.taskcenter.exception.BadRequestException;
import com.taskcenter.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.UUID;

@Service
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
