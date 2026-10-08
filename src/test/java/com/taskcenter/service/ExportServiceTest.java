package com.taskcenter.service;

import com.taskcenter.model.Priority;
import com.taskcenter.model.Task;
import com.taskcenter.model.User;
import com.taskcenter.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private WorkspaceAuthorizationService authorizationService;

    private ExportService exportService;
    private User currentUser;

    @BeforeEach
    void setUp() {
        exportService = new ExportService(taskRepository, authorizationService);
        currentUser = User.builder().id("user-1").name("admin").build();
    }

    @Test
    void exportWorkspaceTasksToCsv_success() {
        String workspaceId = "ws-123";

        User assignee = User.builder().id("dev-1").name("otabek").build();
        Task task = Task.builder()
                .id("task-1")
                .publicId("TASK-101")
                .title("Frontend va Backend integratsiyasi")
                .columnId("col-inprogress")
                .priority(Priority.HIGH)
                .assignees(Set.of(assignee))
                .build();

        when(taskRepository.findByWorkspaceIdWithAssignees(workspaceId)).thenReturn(List.of(task));

        String csv = exportService.exportWorkspaceTasksToCsv(workspaceId, currentUser);

        verify(authorizationService).checkAccess(workspaceId, currentUser);

        String[] lines = csv.split("\n");
        assertThat(lines).hasSize(2);

        // 1. Sarlavhani tekshirish
        assertThat(lines[0]).isEqualTo("ID,Vazifa nomi,Holati (Column),Muhimlik,Ijrochilar");

        // 2. Bitta task qatori to'g'ri chiqishini tekshirish
        assertThat(lines[1]).isEqualTo("TASK-101,Frontend va Backend integratsiyasi,col-inprogress,HIGH,otabek");
    }

    @Test
    void exportWorkspaceTasksToCsv_withEscapedSpecialCharacters() {
        String workspaceId = "ws-123";

        Task task = Task.builder()
                .id("task-2")
                .publicId("TASK-102")
                .title("Xato: login \"tugmasi\", ishlamayapti")
                .columnId("col-todo")
                .priority(Priority.MEDIUM)
                .assignees(Set.of())
                .build();

        when(taskRepository.findByWorkspaceIdWithAssignees(workspaceId)).thenReturn(List.of(task));

        String csv = exportService.exportWorkspaceTasksToCsv(workspaceId, currentUser);

        String[] lines = csv.split("\n");
        assertThat(lines[0]).isEqualTo("ID,Vazifa nomi,Holati (Column),Muhimlik,Ijrochilar");
        assertThat(lines[1]).isEqualTo("TASK-102,\"Xato: login \"\"tugmasi\"\", ishlamayapti\",col-todo,MEDIUM,");
    }
}
