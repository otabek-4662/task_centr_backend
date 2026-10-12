package com.taskcenter.repository;

import com.taskcenter.model.EmailJob;
import com.taskcenter.model.EmailJobStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface EmailJobRepository extends JpaRepository<EmailJob, String> {

    @Query("SELECT j FROM EmailJob j WHERE j.status = :status AND j.nextAttemptAt <= :now ORDER BY j.createdAt ASC")
    List<EmailJob> findReadyJobs(
            @Param("status") EmailJobStatus status,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    long countByStatus(EmailJobStatus status);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
    @Query("UPDATE EmailJob j SET j.status = :newStatus, j.attempts = j.attempts + 1, j.updatedAt = :now WHERE j.id = :id AND j.status = :expectedStatus")
    int claimJobAtomic(
            @Param("id") String id,
            @Param("expectedStatus") EmailJobStatus expectedStatus,
            @Param("newStatus") EmailJobStatus newStatus,
            @Param("now") LocalDateTime now
    );

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
    @Query("UPDATE EmailJob j SET " +
           "j.attempts = j.attempts + 1, " +
           "j.status = CASE WHEN (j.attempts + 1 >= j.maxAttempts) THEN :failedStatus ELSE :pendingStatus END, " +
           "j.lastError = CASE WHEN (j.attempts + 1 >= j.maxAttempts) THEN 'Max retry attempts reached (stuck in PROCESSING)' ELSE j.lastError END, " +
           "j.updatedAt = :now " +
           "WHERE j.status = :processingStatus AND j.updatedAt <= :threshold")
    int recoverStuckProcessingJobs(
            @Param("processingStatus") EmailJobStatus processingStatus,
            @Param("pendingStatus") EmailJobStatus pendingStatus,
            @Param("failedStatus") EmailJobStatus failedStatus,
            @Param("threshold") LocalDateTime threshold,
            @Param("now") LocalDateTime now
    );
}
