package com.taskcenter.repository;

import com.taskcenter.model.UserContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserContactRepository extends JpaRepository<UserContact, String> {

    @Query("SELECT uc FROM UserContact uc JOIN FETCH uc.contactUser WHERE uc.userId = :userId ORDER BY uc.createdAt DESC")
    List<UserContact> findByUserIdWithContactUser(@Param("userId") String userId);

    Optional<UserContact> findByUserIdAndContactUserId(String userId, String contactUserId);

    boolean existsByUserIdAndContactUserId(String userId, String contactUserId);

    @Modifying
    @Query("DELETE FROM UserContact uc WHERE uc.userId = :userId AND uc.contactUserId = :contactUserId")
    void deleteByUserIdAndContactUserId(@Param("userId") String userId, @Param("contactUserId") String contactUserId);

    @Query("SELECT uc.contactUserId FROM UserContact uc WHERE uc.userId = :userId")
    List<String> findContactUserIdsByUserId(@Param("userId") String userId);
}
