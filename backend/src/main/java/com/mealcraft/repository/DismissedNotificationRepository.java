package com.mealcraft.repository;

import com.mealcraft.model.DismissedNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DismissedNotificationRepository extends JpaRepository<DismissedNotification, Long> {

    @Query("SELECT d.referenceId FROM DismissedNotification d WHERE d.user.id = :userId AND d.notificationType = :type")
    List<String> findDismissedReferenceIds(@Param("userId") Long userId, @Param("type") String type);

    Optional<DismissedNotification> findByUser_IdAndNotificationTypeAndReferenceId(
        Long userId, String notificationType, String referenceId);

    boolean existsByUser_IdAndNotificationTypeAndReferenceId(Long userId, String notificationType, String referenceId);
}
