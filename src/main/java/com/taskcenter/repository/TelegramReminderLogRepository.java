package com.taskcenter.repository;

import com.taskcenter.model.TelegramReminderLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface TelegramReminderLogRepository extends JpaRepository<TelegramReminderLog, Long> {

    boolean existsByTaskIdAndUserIdAndType(String taskId, String userId, TelegramReminderLog.ReminderType type);

    boolean existsByLogKey(String logKey);

    Optional<TelegramReminderLog> findByLogKey(String logKey);

    List<TelegramReminderLog> findByTaskId(String taskId);

    @Modifying
    @Transactional
    @Query("DELETE FROM TelegramReminderLog r WHERE r.taskId = :taskId")
    void deleteByTaskId(@Param("taskId") String taskId);

    @Modifying
    @Transactional
    @Query("DELETE FROM TelegramReminderLog r WHERE r.logKey = :logKey")
    void deleteByLogKey(@Param("logKey") String logKey);

    List<TelegramReminderLog> findByUserIdAndTypeAndTaskIdIn(String userId, TelegramReminderLog.ReminderType type, List<String> taskIds);

    @Modifying
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query(value = "INSERT INTO telegram_reminder_log (log_key, type, sent_at) " +
                   "VALUES (:logKey, :type, CURRENT_TIMESTAMP) " +
                   "ON CONFLICT (log_key) WHERE log_key IS NOT NULL DO NOTHING", nativeQuery = true)
    int claimLogKey(@Param("logKey") String logKey, @Param("type") String type);

    @Modifying
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query(value = "INSERT INTO telegram_reminder_log (task_id, user_id, type, sent_at) " +
                   "VALUES (:taskId, :userId, :type, CURRENT_TIMESTAMP) " +
                   "ON CONFLICT DO NOTHING", nativeQuery = true)
    int claimTaskReminder(@Param("taskId") String taskId, @Param("userId") String userId, @Param("type") String type);
}
