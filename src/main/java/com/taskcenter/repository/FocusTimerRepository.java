package com.taskcenter.repository;

import com.taskcenter.model.FocusTimer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FocusTimerRepository extends JpaRepository<FocusTimer, String> {
    Optional<FocusTimer> findByUserIdAndIsActiveTrue(String userId);
    List<FocusTimer> findAllByUserId(String userId);
}
