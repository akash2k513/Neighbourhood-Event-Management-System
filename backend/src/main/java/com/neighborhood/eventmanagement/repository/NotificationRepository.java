package com.neighborhood.eventmanagement.repository;

import com.neighborhood.eventmanagement.entity.Notification;
import com.neighborhood.eventmanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserOrderByCreatedAtDesc(User user);

    List<Notification> findByUserAndIsReadFalseOrderByCreatedAtDesc(User user);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user = :user")
    void markAllReadByUser(@Param("user") User user);

    long countByUserAndIsReadFalse(User user);

    @Query("SELECT n FROM Notification n WHERE n.user = :user AND n.priority = :priority ORDER BY n.createdAt DESC")
    List<Notification> findByUserAndPriority(@Param("user") User user,
                                             @Param("priority") Notification.Priority priority);
}