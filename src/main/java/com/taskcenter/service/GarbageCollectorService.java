package com.taskcenter.service;

import com.taskcenter.service.storage.FileStorageService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class GarbageCollectorService {

    private static final Logger log = LoggerFactory.getLogger(GarbageCollectorService.class);

    private final JdbcTemplate jdbcTemplate;
    private final FileStorageService fileStorageService;

    public GarbageCollectorService(JdbcTemplate jdbcTemplate, FileStorageService fileStorageService) {
        this.jdbcTemplate = jdbcTemplate;
        this.fileStorageService = fileStorageService;
    }

    /**
     * Har kuni tunda soat 03:00 (Asia/Tashkent) da ishga tushadi.
     * 30 kundan ko'p vaqt oldin o'chirilgan (deleted_at) ma'lumotlarni bazadan butunlay o'chirib tashlaydi.
     */
    @Scheduled(cron = "0 0 3 * * ?", zone = "Asia/Tashkent")
    @SchedulerLock(name = "GarbageCollectorService_cleanUpOldDeletedRecords", lockAtLeastFor = "5m", lockAtMostFor = "30m")
    @Transactional
    public void cleanUpOldDeletedRecords() {
        log.info("Garbage Collector: 30 kundan eski o'chirilgan ma'lumotlarni tozalash boshlandi...");

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);

        try {
            List<String> storagePaths = jdbcTemplate.queryForList(
                    "SELECT a.storage_path FROM attachments a JOIN tasks t ON a.task_id = t.id WHERE t.deleted_at < ?",
                    String.class, cutoffDate);

            int tasksDeleted = jdbcTemplate.update("DELETE FROM tasks WHERE deleted_at < ?", cutoffDate);
            int columnsDeleted = jdbcTemplate.update("DELETE FROM board_columns WHERE deleted_at < ?", cutoffDate);
            int workspacesDeleted = jdbcTemplate.update("DELETE FROM workspaces WHERE deleted_at < ?", cutoffDate);

            if (!storagePaths.isEmpty()) {
                if (TransactionSynchronizationManager.isSynchronizationActive()) {
                    TransactionSynchronizationManager.registerSynchronization(
                            new TransactionSynchronization() {
                                @Override
                                public void afterCommit() {
                                    deletePhysicalFiles(storagePaths, true);
                                }
                            }
                    );
                } else {
                    deletePhysicalFiles(storagePaths, false);
                }
            }

            log.info("Garbage Collector natijasi: tasks={}, columns={}, workspaces={}",
                    tasksDeleted, columnsDeleted, workspacesDeleted);
        } catch (Exception e) {
            log.error("Garbage Collector ishlashida xatolik: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Har kuni tunda soat 03:15 (Asia/Tashkent) da "Done" (is_done = true) ustunida
     * 30 kundan ortiq o'zgarishsiz turgan vazifalarni avtomatik arxivga (is_archived = true) oladi.
     */
    @Scheduled(cron = "0 15 3 * * ?", zone = "Asia/Tashkent")
    @SchedulerLock(name = "GarbageCollectorService_autoArchiveOldDoneTasks", lockAtLeastFor = "5m", lockAtMostFor = "30m")
    @Transactional
    public void autoArchiveOldDoneTasks() {
        LocalDateTime archiveCutoff = LocalDateTime.now(ZoneOffset.UTC).minusDays(30);
        try {
            int archivedCount = jdbcTemplate.update(
                    "UPDATE tasks SET is_archived = true " +
                    "WHERE (is_archived = false OR is_archived IS NULL) " +
                    "  AND deleted_at IS NULL " +
                    "  AND updated_at < ? " +
                    "  AND column_id IN (SELECT id FROM board_columns WHERE is_done = true AND deleted_at IS NULL)",
                    archiveCutoff);
            if (archivedCount > 0) {
                log.info("Auto-Archive: 30 kundan eski {} ta tugallangan (Done) vazifa arxivga o'tkazildi.", archivedCount);
            }
        } catch (Exception e) {
            log.warn("Eski Done vazifalarni avtomatik arxivlashda xatolik: {}", e.getMessage());
        }
    }

    /**
     * Har kuni tunda soat 03:30 (Asia/Tashkent) da muddati o'tgan va bekor qilingan
     * refresh tokenlar, parol tiklash tokenlari, eski eslatma loglari va 180 kundan eski
     * texnik activity yozuvlarini tozalaydi.
     */
    @Scheduled(cron = "0 30 3 * * ?", zone = "Asia/Tashkent")
    @SchedulerLock(name = "GarbageCollectorService_cleanUpExpiredTokensAndLogs", lockAtLeastFor = "5m", lockAtMostFor = "30m")
    @Transactional
    public void cleanUpExpiredTokensAndLogs() {
        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime revokedCutoff = nowUtc.minusDays(7);
        LocalDateTime reminderLogCutoff = nowUtc.minusDays(60);
        LocalDateTime activityArchiveCutoff = nowUtc.minusDays(180);

        try {
            int refreshDeleted = jdbcTemplate.update(
                    "DELETE FROM refresh_tokens WHERE expiry_date < ? OR (revoked = true AND created_at < ?)",
                    nowUtc, revokedCutoff);
            int resetDeleted = jdbcTemplate.update(
                    "DELETE FROM password_reset_tokens WHERE expiry_date < ? OR used = true",
                    nowUtc);
            int reminderLogsDeleted = jdbcTemplate.update(
                    "DELETE FROM telegram_reminder_log WHERE sent_at < ?",
                    reminderLogCutoff);
            int oldActivitiesDeleted = jdbcTemplate.update(
                    "DELETE FROM task_activities WHERE created_at < ?",
                    activityArchiveCutoff);

            log.info("Token & Log GC natijasi: refreshTokens={}, resetTokens={}, reminderLogs={}, oldActivities={}",
                    refreshDeleted, resetDeleted, reminderLogsDeleted, oldActivitiesDeleted);
        } catch (Exception e) {
            log.warn("Token va loglarni tozalashda xatolik: {}", e.getMessage());
        }
    }

    private void deletePhysicalFiles(List<String> storagePaths, boolean afterCommit) {
        for (String path : storagePaths) {
            try {
                fileStorageService.deleteFile(path);
            } catch (Exception ex) {
                log.error("Fayl o'chirishda xatolik{}: {} - {}",
                        afterCommit ? " (afterCommit)" : "", path, ex.getMessage());
            }
        }
    }
}
