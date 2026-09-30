package com.taskcenter.repository;

import com.taskcenter.model.TelegramReminderLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface TelegramReminderLogRepository extends JpaRepository<TelegramReminderLog, Long> {

    boolean existsByTaskIdAndUserIdAndType(String taskId, String userId, TelegramReminderLog.ReminderType type);

    List<TelegramReminderLog> findByTaskId(String taskId);

    @Modifying
    @Transactional
    @Query("DELETE FROM TelegramReminderLog r WHERE r.taskId = :taskId")
    void deleteByTaskId(@Param("taskId") String taskId);
}
