package com.taskcenter.service;

import com.taskcenter.dto.GitHubPushEventDto;
import com.taskcenter.exception.ForbiddenException;
import com.taskcenter.model.BoardColumn;
import com.taskcenter.model.Comment;
import com.taskcenter.model.Task;
import com.taskcenter.model.Workspace;
import com.taskcenter.repository.ColumnRepository;
import com.taskcenter.repository.CommentRepository;
import com.taskcenter.repository.TaskRepository;
import com.taskcenter.repository.WorkspaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private static final String TASK_REGEX = "(?i)(fixes|closes|resolves)\\s+([A-Z0-9]+-\\d+)";
    private static final Pattern PATTERN = Pattern.compile(TASK_REGEX);

    private final TaskRepository taskRepository;
    private final ColumnRepository columnRepository;
    private final CommentRepository commentRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WebSocketNotifier webSocketNotifier;
    private final String webhookSecret;

    public WebhookService(TaskRepository taskRepository,
                          ColumnRepository columnRepository,
                          CommentRepository commentRepository,
                          WorkspaceRepository workspaceRepository,
                          WebSocketNotifier webSocketNotifier,
                          @Value("${github.webhook-secret:}") String webhookSecret) {
        this.taskRepository = taskRepository;
        this.columnRepository = columnRepository;
        this.commentRepository = commentRepository;
        this.workspaceRepository = workspaceRepository;
        this.webSocketNotifier = webSocketNotifier;
        this.webhookSecret = webhookSecret;
    }

    /**
     * GitHub har bir so'rovni X-Hub-Signature-256 = "sha256=" + HMAC-SHA256(secret, body) bilan imzolaydi.
     * Imzo mos kelmasa, so'rov GitHub dan emas — uni rad etamiz.
     */
    public void verifySignature(byte[] rawBody, String signatureHeader) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.warn("GITHUB_WEBHOOK_SECRET sozlanmagan — webhook so'rovi rad etildi");
            throw new ForbiddenException("Webhook sozlanmagan");
        }
        if (signatureHeader == null || !signatureHeader.startsWith("sha256=")) {
            throw new ForbiddenException("Webhook imzosi yo'q");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = "sha256=" + HexFormat.of().formatHex(mac.doFinal(rawBody));
            // MessageDigest.isEqual — vaqt bo'yicha xavfsiz taqqoslash (timing attack dan himoya)
            if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                    signatureHeader.getBytes(StandardCharsets.UTF_8))) {
                throw new ForbiddenException("Webhook imzosi noto'g'ri");
            }
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC hisoblab bo'lmadi", e);
        }
    }

    @Transactional
    public void processGitHubPushEvent(GitHubPushEventDto payload) {
        log.info("GitHub Webhook qabul qilindi. Repository: {}", 
            payload.getRepository() != null ? payload.getRepository().getFull_name() : "Noma'lum");

        if (payload.getCommits() == null || payload.getCommits().isEmpty()) {
            return;
        }

        for (GitHubPushEventDto.Commit commit : payload.getCommits()) {
            if (commit.getMessage() == null) continue;

            Matcher matcher = PATTERN.matcher(commit.getMessage());
            while (matcher.find()) {
                String action = matcher.group(1);
                String publicId = matcher.group(2).toUpperCase();

                log.info("Commitda vazifa topildi: {} -> Action: {}", publicId, action);
                processTaskFromCommit(publicId, commit, payload.getSender());
            }
        }
    }

    private void processTaskFromCommit(String publicId, GitHubPushEventDto.Commit commit, GitHubPushEventDto.Sender sender) {
        Optional<Task> taskOpt = taskRepository.findByPublicId(publicId);
        if (taskOpt.isEmpty()) {
            log.warn("Vazifa topilmadi: {}", publicId);
            return;
        }

        Task task = taskOpt.get();
        Workspace workspace = workspaceRepository.findById(task.getWorkspaceId()).orElse(null);
        if (workspace == null) return;

        // 1. Taskni "Done" (Bajarildi) ustuniga o'tkazish
        List<BoardColumn> columns = columnRepository.findByWorkspaceIdOrderByOrderAsc(workspace.getId());
        BoardColumn doneColumn = null;
        for (BoardColumn col : columns) {
            if (Boolean.TRUE.equals(col.getIsDefault()) && col.getOrder() == 3) {
                doneColumn = col;
                break;
            }
        }

        if (doneColumn != null && !task.getColumnId().equals(doneColumn.getId())) {
            task.setColumnId(doneColumn.getId());
            taskRepository.save(task);
            log.info("Task {} 'Done' ustuniga o'tkazildi", publicId);
            
            // WebSocket orqali frontendga xabar berish
            com.taskcenter.dto.WebSocketEvent<Task> event = new com.taskcenter.dto.WebSocketEvent<>("TASK_UPDATED", workspace.getId(), task);
            webSocketNotifier.notifyWorkspace(workspace.getId(), event);
        }

        // 2. Taskga izoh (Comment) qo'shish
        String authorName = (commit.getAuthor() != null && commit.getAuthor().getName() != null) 
                            ? commit.getAuthor().getName() 
                            : (sender != null ? sender.getLogin() : "GitHub User");
                            
        String commentText = String.format("🤖 **GitHub Avtomatizatsiyasi:**\nUshbu vazifa `%s` tomonidan hal qilindi.\n\n**Commit:** [%s](%s)",
                authorName,
                commit.getMessage(),
                commit.getUrl());

        Comment comment = Comment.builder()
                .taskId(task.getId())
                .authorId(workspace.getOwnerId()) // Tizim yuboruvchisi o'rniga loyiha egasi
                .content(commentText)
                .build();
        commentRepository.save(comment);
        
        com.taskcenter.dto.WebSocketEvent<Comment> commentEvent = new com.taskcenter.dto.WebSocketEvent<>("COMMENT_ADDED", workspace.getId(), comment);
        webSocketNotifier.notifyWorkspace(workspace.getId(), commentEvent);
    }
}
