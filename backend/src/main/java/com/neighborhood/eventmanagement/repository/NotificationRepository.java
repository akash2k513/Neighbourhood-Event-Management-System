package com.neighborhood.eventmanagement.repository;

import com.neighborhood.eventmanagement.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}