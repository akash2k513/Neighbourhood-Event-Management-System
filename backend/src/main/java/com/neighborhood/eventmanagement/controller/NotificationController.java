package com.neighborhood.eventmanagement.controller;

import com.neighborhood.eventmanagement.entity.Notification;
import com.neighborhood.eventmanagement.entity.User;
import com.neighborhood.eventmanagement.exception.ResourceNotFoundException;
import com.neighborhood.eventmanagement.exception.UnauthorizedAccessException;
import com.neighborhood.eventmanagement.repository.NotificationRepository;
import com.neighborhood.eventmanagement.repository.UserRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "APIs for managing user notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationController(NotificationRepository notificationRepository,
                                   UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Operation(summary = "Get all notifications for current user")
    @GetMapping
    public ResponseEntity<List<Notification>> getMyNotifications(Authentication authentication) {
        User user = getUser(authentication);
        return ResponseEntity.ok(notificationRepository.findByUserOrderByCreatedAtDesc(user));
    }

    @Operation(summary = "Get unread notifications for current user")
    @GetMapping("/unread")
    public ResponseEntity<List<Notification>> getUnread(Authentication authentication) {
        User user = getUser(authentication);
        return ResponseEntity.ok(notificationRepository.findByUserAndIsReadFalseOrderByCreatedAtDesc(user));
    }

    @Operation(summary = "Mark a notification as read")
    @PatchMapping("/{id}/read")
    public ResponseEntity<String> markRead(@PathVariable Long id, Authentication authentication) {
        User user = getUser(authentication);
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + id));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedAccessException("Access denied.");
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);
        return ResponseEntity.ok("Notification marked as read.");
    }

    @Operation(summary = "Mark all notifications as read")
    @PatchMapping("/read-all")
    public ResponseEntity<String> markAllRead(Authentication authentication) {
        User user = getUser(authentication);
        notificationRepository.markAllReadByUser(user);
        return ResponseEntity.ok("All notifications marked as read.");
    }

    @Operation(summary = "Delete a notification")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteNotification(@PathVariable Long id, Authentication authentication) {
        User user = getUser(authentication);
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + id));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedAccessException("Access denied.");
        }

        notificationRepository.delete(notification);
        return ResponseEntity.ok("Notification deleted.");
    }

    private User getUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }
}
