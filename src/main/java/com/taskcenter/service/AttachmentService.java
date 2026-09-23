package com.taskcenter.service;

import com.taskcenter.dto.AttachmentDto;
import com.taskcenter.exception.ForbiddenException;
import com.taskcenter.exception.ResourceNotFoundException;
import com.taskcenter.model.Attachment;
import com.taskcenter.model.Task;
import com.taskcenter.model.User;
import com.taskcenter.repository.AttachmentRepository;
import com.taskcenter.repository.TaskRepository;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final TaskRepository taskRepository;
    private final FileStorageService fileStorageService;
    private final WorkspaceAuthorizationService authorizationService;

    public AttachmentService(AttachmentRepository attachmentRepository,
                             TaskRepository taskRepository,
                             FileStorageService fileStorageService,
                             WorkspaceAuthorizationService authorizationService) {
        this.attachmentRepository = attachmentRepository;
        this.taskRepository = taskRepository;
        this.fileStorageService = fileStorageService;
        this.authorizationService = authorizationService;
    }

    @Transactional
    public AttachmentDto uploadAttachment(String taskId, MultipartFile file, User currentUser) {
        Task task = getTask(taskId);
        authorizationService.checkAccess(task.getWorkspaceId(), currentUser);

        String storagePath = fileStorageService.storeFile(file, "tasks/" + taskId);

        Attachment attachment = Attachment.builder()
                .taskId(taskId)
                .uploaderId(currentUser.getId())
                .uploader(currentUser)
                .fileName(file.getOriginalFilename() != null ? file.getOriginalFilename() : "file")
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .storagePath(storagePath)
                .build();

        Attachment saved = attachmentRepository.save(attachment);
        return AttachmentDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<AttachmentDto> getAttachments(String taskId, User currentUser) {
        Task task = getTask(taskId);
        authorizationService.checkAccess(task.getWorkspaceId(), currentUser);

        return attachmentRepository.findByTaskIdOrderByCreatedAtDesc(taskId)
                .stream()
                .map(AttachmentDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Attachment getAttachment(String attachmentId, User currentUser) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Fayl topilmadi: " + attachmentId));

        Task task = getTask(attachment.getTaskId());
        authorizationService.checkAccess(task.getWorkspaceId(), currentUser);

        return attachment;
    }

    public Resource loadFileAsResource(Attachment attachment) {
        return fileStorageService.loadFileAsResource(attachment.getStoragePath());
    }

    @Transactional
    public void deleteAttachment(String attachmentId, User currentUser) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Fayl topilmadi: " + attachmentId));

        Task task = getTask(attachment.getTaskId());

        boolean isUploader = attachment.getUploaderId().equals(currentUser.getId());
        boolean isOwnerOrAdmin = authorizationService.isOwnerOrAdmin(task.getWorkspaceId(), currentUser.getId());

        if (!isUploader && !isOwnerOrAdmin) {
            throw new ForbiddenException("Ushbu biriktirilgan faylni o'chirish uchun ruxsat yo'q");
        }

        fileStorageService.deleteFile(attachment.getStoragePath());
        attachmentRepository.delete(attachment);
    }

    private Task getTask(String taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task topilmadi: " + taskId));
    }
}
