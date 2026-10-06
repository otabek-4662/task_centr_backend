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
}
