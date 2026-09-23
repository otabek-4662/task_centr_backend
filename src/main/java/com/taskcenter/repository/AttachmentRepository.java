package com.taskcenter.repository;

import com.taskcenter.model.Attachment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, String> {

    @EntityGraph(attributePaths = {"uploader"})
    List<Attachment> findByTaskIdOrderByCreatedAtDesc(String taskId);

    @EntityGraph(attributePaths = {"uploader"})
    Optional<Attachment> findWithUploaderById(String id);
}
