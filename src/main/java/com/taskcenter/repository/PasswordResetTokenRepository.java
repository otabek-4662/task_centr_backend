package com.taskcenter.repository;

import com.taskcenter.model.PasswordResetToken;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, String> {

    @EntityGraph(attributePaths = {"user"})
    Optional<PasswordResetToken> findByToken(String token);
}
