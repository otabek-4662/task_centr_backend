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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private WorkspaceAuthorizationService authorizationService;

    @InjectMocks
    private CommentService commentService;

    private final String workspaceId = "ws-1";
    private final String taskId = "task-1";
    private final String authorId = "user-author";
    private final String strangerId = "user-stranger";

    private Task task;
    private User author;
    private User stranger;
    private Comment comment;

    @BeforeEach
    void setUp() {
        task = Task.builder()
                .id(taskId)
                .workspaceId(workspaceId)
                .title("Test Task")
                .build();

        author = User.builder().id(authorId).name("author").fullName("Author User").build();
        stranger = User.builder().id(strangerId).name("stranger").fullName("Stranger User").build();

        comment = Comment.builder()
                .id("comment-1")
                .taskId(taskId)
                .authorId(authorId)
                .author(author)
                .content("Initial comment")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Taskka muvaffaqiyatli izoh qo'shiladi")
    void addComment_success() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));        org.mockito.Mockito.lenient().when(taskRepository.findWorkspaceIdById(taskId)).thenReturn(Optional.ofNullable("ws1"));
        org.mockito.Mockito.lenient().when(authorizationService.checkAccess(workspaceId, author)).thenReturn(new com.taskcenter.model.Workspace());

        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
            Comment c = inv.getArgument(0);
            c.setId("saved-comment");
            return c;
        });

        CommentCreateRequest request = CommentCreateRequest.builder().content("Awesome feature!").build();
        CommentDto result = commentService.addComment(taskId, request, author);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo("Awesome feature!");
        assertThat(result.getAuthorName()).isEqualTo("author");
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    @DisplayName("Task izohlari ro'yxati sahifalab olinadi")
    void getComments_success() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));        org.mockito.Mockito.lenient().when(taskRepository.findWorkspaceIdById(taskId)).thenReturn(Optional.ofNullable("ws1"));
        org.mockito.Mockito.lenient().when(authorizationService.checkAccess(workspaceId, author)).thenReturn(new com.taskcenter.model.Workspace());

        Pageable pageable = PageRequest.of(0, 10);
        Page<Comment> commentPage = new PageImpl<>(List.of(comment), pageable, 1);
        when(commentRepository.findByTaskIdOrderByCreatedAtAsc(taskId, pageable)).thenReturn(commentPage);

        Page<CommentDto> result = commentService.getComments(taskId, pageable, author);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getContent()).isEqualTo("Initial comment");
    }

    @Test
    @DisplayName("Muallif o'z izohini muvaffaqiyatli tahrirlay oladi")
    void updateComment_authorCanUpdate() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));        org.mockito.Mockito.lenient().when(taskRepository.findWorkspaceIdById(taskId)).thenReturn(Optional.ofNullable("ws1"));
        org.mockito.Mockito.lenient().when(authorizationService.checkAccess(workspaceId, author)).thenReturn(new com.taskcenter.model.Workspace());
        when(commentRepository.findWithAuthorById("comment-1")).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));

        CommentUpdateRequest request = CommentUpdateRequest.builder().content("Updated text").build();
        CommentDto result = commentService.updateComment(taskId, "comment-1", request, author);

        assertThat(result.getContent()).isEqualTo("Updated text");
    }

    @Test
    @DisplayName("Boshqa foydalanuvchi izohni tahrirlamoqchi bo'lsa ForbiddenException oladi")
    void updateComment_strangerCannotUpdate() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));        org.mockito.Mockito.lenient().when(taskRepository.findWorkspaceIdById(taskId)).thenReturn(Optional.ofNullable("ws1"));
        org.mockito.Mockito.lenient().when(authorizationService.checkAccess(workspaceId, stranger)).thenReturn(new com.taskcenter.model.Workspace());
        when(commentRepository.findWithAuthorById("comment-1")).thenReturn(Optional.of(comment));

        CommentUpdateRequest request = CommentUpdateRequest.builder().content("Malicious text").build();

        assertThatThrownBy(() -> commentService.updateComment(taskId, "comment-1", request, stranger))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Faqat o'z izohingizni tahrirlashingiz mumkin");
    }

    @Test
    @DisplayName("Muallif o'z izohini o'chira oladi")
    void deleteComment_authorCanDelete() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));        org.mockito.Mockito.lenient().when(taskRepository.findWorkspaceIdById(taskId)).thenReturn(Optional.ofNullable("ws1"));
        when(commentRepository.findById("comment-1")).thenReturn(Optional.of(comment));

        commentService.deleteComment(taskId, "comment-1", author);

        verify(commentRepository).delete(comment);
    }

    @Test
    @DisplayName("Workspace egasi / admini boshqa a'zoning izohini moderator sifatida o'chira oladi")
    void deleteComment_adminCanDelete() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));        org.mockito.Mockito.lenient().when(taskRepository.findWorkspaceIdById(taskId)).thenReturn(Optional.ofNullable("ws1"));
        when(commentRepository.findById("comment-1")).thenReturn(Optional.of(comment));
        when(authorizationService.isOwnerOrAdmin(workspaceId, strangerId)).thenReturn(true);

        commentService.deleteComment(taskId, "comment-1", stranger);

        verify(commentRepository).delete(comment);
    }

    @Test
    @DisplayName("Oddiy a'zo boshqa a'zoning izohini o'chira olmaydi (ForbiddenException)")
    void deleteComment_regularMemberCannotDeleteOtherComment() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));        org.mockito.Mockito.lenient().when(taskRepository.findWorkspaceIdById(taskId)).thenReturn(Optional.ofNullable("ws1"));
        when(commentRepository.findById("comment-1")).thenReturn(Optional.of(comment));
        when(authorizationService.isOwnerOrAdmin(workspaceId, strangerId)).thenReturn(false);

        assertThatThrownBy(() -> commentService.deleteComment(taskId, "comment-1", stranger))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Ushbu izohni o'chirish uchun ruxsat yo'q");
    }
}
