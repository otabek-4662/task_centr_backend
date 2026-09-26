package com.taskcenter.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class GarbageCollectorService {

    private static final Logger log = LoggerFactory.getLogger(GarbageCollectorService.class);

    private final JdbcTemplate jdbcTemplate;

    public GarbageCollectorService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Har kuni tunda soat 03:00 da ishga tushadi
     * 30 kundan ko'p vaqt oldin o'chirilgan (deleted_at) ma'lumotlarni bazadan butunlay o'chirib tashlaydi.
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @org.springframework.transaction.annotation.Transactional
    public void cleanUpOldDeletedRecords() {
        log.info("Garbage Collector: 30 kundan eski o'chirilgan ma'lumotlarni tozalash boshlandi...");
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        
        try {
            int tasksDeleted = jdbcTemplate.update("DELETE FROM tasks WHERE deleted_at < ?", cutoffDate);
            int columnsDeleted = jdbcTemplate.update("DELETE FROM board_columns WHERE deleted_at < ?", cutoffDate);
            // sprints jadvalida deleted_at yo'q (sprint darhol o'chiriladi), shuning uchun u bu yerda yo'q
            int workspacesDeleted = jdbcTemplate.update("DELETE FROM workspaces WHERE deleted_at < ?", cutoffDate);
            
            log.info("Garbage Collector natijasi:");
            log.info(" - Tasks o'chirildi: {}", tasksDeleted);
            log.info(" - Columns o'chirildi: {}", columnsDeleted);
            log.info(" - Workspaces o'chirildi: {}", workspacesDeleted);
        } catch (Exception e) {
            log.error("Garbage Collector ishlashida xatolik: {}", e.getMessage());
        }
    }
}
