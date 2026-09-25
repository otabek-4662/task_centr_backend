package com.taskcenter.service;

import com.taskcenter.dto.AttachmentDto;
import com.taskcenter.exception.ForbiddenException;
import com.taskcenter.model.Attachment;
import com.taskcenter.model.Task;
import com.taskcenter.model.User;
import com.taskcenter.repository.AttachmentRepository;
import com.taskcenter.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceTest {

    @Mock
    private AttachmentRepository attachmentRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private WorkspaceAuthorizationService authorizationService;

    @InjectMocks
    private AttachmentService attachmentService;

    private final String workspaceId = "ws-1";
    private final String taskId = "task-1";
    private final String uploaderId = "user-1";
    private final String strangerId = "user-stranger";

    private Task task;
    private User uploader;
    private User stranger;
    private Attachment attachment;

    @BeforeEach
    void setUp() {
        task = Task.builder().id(taskId).workspaceId(workspaceId).title("Test Task").build();
        uploader = User.builder().id(uploaderId).name("elshod").build();
        stranger = User.builder().id(strangerId).name("stranger").build();

        attachment = Attachment.builder()
                .id("att-1")
                .taskId(taskId)
                .uploaderId(uploaderId)
                .uploader(uploader)
                .fileName("screenshot.png")
                .fileType("image/png")
                .fileSize(1024L)
                .storagePath("tasks/task-1/uuid_screenshot.png")
                .build();
    }

    @Test
    @DisplayName("Fayl muvaffaqiyatli yuklanadi")
    void uploadAttachment_success() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));        org.mockito.Mockito.lenient().when(taskRepository.findWorkspaceIdById(taskId)).thenReturn(Optional.ofNullable("ws1"));
        org.mockito.Mockito.lenient().when(authorizationService.checkAccess(workspaceId, uploader)).thenReturn(new com.taskcenter.model.Workspace());
        when(fileStorageService.storeFile(any(), eq("tasks/" + taskId))).thenReturn("tasks/task-1/uuid_screenshot.png");
        when(attachmentRepository.save(any(Attachment.class))).thenAnswer(inv -> {
            Attachment a = inv.getArgument(0);
            a.setId("saved-att-1");
            return a;
        });

        MockMultipartFile file = new MockMultipartFile("file", "screenshot.png", "image/png", "fake-image".getBytes());
        AttachmentDto result = attachmentService.uploadAttachment(taskId, file, uploader);

        assertThat(result).isNotNull();
        assertThat(result.getFileName()).isEqualTo("screenshot.png");
        assertThat(result.getUploaderName()).isEqualTo("elshod");
        verify(attachmentRepository).save(any(Attachment.class));
    }

    @Test
    @DisplayName("Task fayllari ro'yxatini olish")
    void getAttachments_success() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));        org.mockito.Mockito.lenient().when(taskRepository.findWorkspaceIdById(taskId)).thenReturn(Optional.ofNullable("ws1"));
        org.mockito.Mockito.lenient().when(authorizationService.checkAccess(workspaceId, uploader)).thenReturn(new com.taskcenter.model.Workspace());
        when(attachmentRepository.findByTaskIdOrderByCreatedAtDesc(taskId)).thenReturn(List.of(attachment));

        List<AttachmentDto> result = attachmentService.getAttachments(taskId, uploader);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFileName()).isEqualTo("screenshot.png");
    }

    @Test
    @DisplayName("Yuklagan shaxs faylni o'chira oladi")
    void deleteAttachment_uploaderCanDelete() {
        when(attachmentRepository.findById("att-1")).thenReturn(Optional.of(attachment));
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));        org.mockito.Mockito.lenient().when(taskRepository.findWorkspaceIdById(taskId)).thenReturn(Optional.ofNullable("ws1"));

        attachmentService.deleteAttachment("att-1", uploader);

        verify(fileStorageService).deleteFile("tasks/task-1/uuid_screenshot.png");
        verify(attachmentRepository).delete(attachment);
    }

    @Test
    @DisplayName("Boshqa foydalanuvchi faylni o'chira olmaydi (ForbiddenException)")
    void deleteAttachment_strangerCannotDelete() {
        when(attachmentRepository.findById("att-1")).thenReturn(Optional.of(attachment));
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));        org.mockito.Mockito.lenient().when(taskRepository.findWorkspaceIdById(taskId)).thenReturn(Optional.ofNullable("ws1"));
        when(authorizationService.isOwnerOrAdmin(workspaceId, strangerId)).thenReturn(false);

        assertThatThrownBy(() -> attachmentService.deleteAttachment("att-1", stranger))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Ushbu biriktirilgan faylni o'chirish uchun ruxsat yo'q");
    }
}
