package com.taskcenter.service;

import com.taskcenter.dto.CommentCreateRequest;
import com.taskcenter.dto.CommentDto;
import com.taskcenter.dto.CommentUpdateRequest;
import com.taskcenter.exception.ForbiddenException;
import com.taskcenter.exception.ResourceNotFoundException;
import com.taskcenter.model.Comment;
import com.taskcenter.model.Task;
import com.taskcenter.model.User;
import com.taskcenter.repository.CommentRepository;
import com.taskcenter.repository.TaskRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final WorkspaceAuthorizationService authorizationService;

    public CommentService(CommentRepository commentRepository,
                          TaskRepository taskRepository,
                          WorkspaceAuthorizationService authorizationService) {
        this.commentRepository = commentRepository;
        this.taskRepository = taskRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional(readOnly = true)
    public Page<CommentDto> getComments(String taskId, Pageable pageable, User currentUser) {
        String workspaceId = getWorkspaceId(taskId);
        authorizationService.checkAccess(workspaceId, currentUser);

        return commentRepository.findByTaskIdOrderByCreatedAtAsc(taskId, pageable)
                .map(CommentDto::fromEntity);
    }

    @Transactional
    public CommentDto addComment(String taskId, CommentCreateRequest req, User currentUser) {
        String workspaceId = getWorkspaceId(taskId);
        authorizationService.checkAccess(workspaceId, currentUser);

        Comment comment = Comment.builder()
                .taskId(taskId)
                .authorId(currentUser.getId())
                .author(currentUser)
                .content(req.getContent())
                .build();

        Comment saved = commentRepository.save(comment);
        return CommentDto.fromEntity(saved);
    }

    @Transactional
    public CommentDto updateComment(String taskId, String commentId, CommentUpdateRequest req, User currentUser) {
        String workspaceId = getWorkspaceId(taskId);
        authorizationService.checkAccess(workspaceId, currentUser);

        Comment comment = commentRepository.findWithAuthorById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Izoh topilmadi: " + commentId));

        if (!comment.getTaskId().equals(taskId)) {
            throw new ResourceNotFoundException("Izoh ushbu taskka tegishli emas");
        }

        if (!comment.getAuthorId().equals(currentUser.getId())) {
            throw new ForbiddenException("Faqat o'z izohingizni tahrirlashingiz mumkin");
        }

        comment.setContent(req.getContent());
        Comment updated = commentRepository.save(comment);
        return CommentDto.fromEntity(updated);
    }

    @Transactional
    public void deleteComment(String taskId, String commentId, User currentUser) {
        String workspaceId = getWorkspaceId(taskId);
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Izoh topilmadi: " + commentId));

        if (!comment.getTaskId().equals(taskId)) {
            throw new ResourceNotFoundException("Izoh ushbu taskka tegishli emas");
        }

        boolean isAuthor = comment.getAuthorId().equals(currentUser.getId());
        boolean isOwnerOrAdmin = authorizationService.isOwnerOrAdmin(workspaceId, currentUser.getId());

        if (!isAuthor && !isOwnerOrAdmin) {
            throw new ForbiddenException("Ushbu izohni o'chirish uchun ruxsat yo'q");
        }

        commentRepository.delete(comment);
    }

    private String getWorkspaceId(String taskId) {
        return taskRepository.findWorkspaceIdById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task topilmadi: " + taskId));
    }
}
