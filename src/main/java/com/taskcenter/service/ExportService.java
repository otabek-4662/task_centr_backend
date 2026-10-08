package com.taskcenter.service;

import com.taskcenter.model.Task;
import com.taskcenter.model.User;
import com.taskcenter.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExportService {

    private final TaskRepository taskRepository;
    private final WorkspaceAuthorizationService authorizationService;

    public ExportService(TaskRepository taskRepository, WorkspaceAuthorizationService authorizationService) {
        this.taskRepository = taskRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional(readOnly = true)
    public String exportWorkspaceTasksToCsv(String workspaceId, User currentUser) {
        authorizationService.checkAccess(workspaceId, currentUser);

        List<Task> tasks = taskRepository.findByWorkspaceIdWithAssignees(workspaceId);

        StringBuilder sb = new StringBuilder();
        sb.append("ID,Vazifa nomi,Holati (Column),Muhimlik,Ijrochilar\n");

        for (Task t : tasks) {
            String assignees = t.getAssignees().stream().map(User::getName).collect(Collectors.joining("; "));
            sb.append(escapeSpecialCharacters(t.getPublicId())).append(",")
              .append(escapeSpecialCharacters(t.getTitle())).append(",")
              .append(escapeSpecialCharacters(t.getColumnId())).append(",")
              .append(t.getPriority().name()).append(",")
              .append(escapeSpecialCharacters(assignees)).append("\n");
        }

        return sb.toString();
    }

    private String escapeSpecialCharacters(String data) {
        if (data == null) return "";
        String escapedData = data.replaceAll("\\R", " ");
        if (data.contains(",") || data.contains("\"") || data.contains("'")) {
            data = data.replace("\"", "\"\"");
            escapedData = "\"" + data + "\"";
        }
        return escapedData;
    }
}
