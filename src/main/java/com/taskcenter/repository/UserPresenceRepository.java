package com.taskcenter.repository;

import com.taskcenter.model.UserPresence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import java.util.List;

@Repository
public interface UserPresenceRepository extends JpaRepository<UserPresence, String> {
    @Query("SELECT u.name FROM UserPresence up JOIN up.user u WHERE up.online = true")
    List<String> findOnlineUsernames();
}
