package com.taskcenter.repository;

import com.taskcenter.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, String> {

    @EntityGraph(attributePaths = {"author"})
    Page<Comment> findByTaskIdOrderByCreatedAtAsc(String taskId, Pageable pageable);

    @EntityGraph(attributePaths = {"author"})
    Optional<Comment> findWithAuthorById(String id);
}
