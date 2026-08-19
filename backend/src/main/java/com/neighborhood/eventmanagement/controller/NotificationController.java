package com.neighborhood.eventmanagement.controller;

import com.neighborhood.eventmanagement.entity.Notification;
import com.neighborhood.eventmanagement.entity.User;
import com.neighborhood.eventmanagement.entity.Zone;
import com.neighborhood.eventmanagement.exception.ResourceNotFoundException;
import com.neighborhood.eventmanagement.exception.UnauthorizedAccessException;
import com.neighborhood.eventmanagement.repository.NotificationRepository;
import com.neighborhood.eventmanagement.repository.UserRepository;
import com.neighborhood.eventmanagement.repository.ZoneRepository;
import com.neighborhood.eventmanagement.service.NotificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "Notification management and delivery (SRS FR8, FR11, 8.9)")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ZoneRepository zoneRepository;
    private final NotificationService notificationService;

    public NotificationController(NotificationRepository notificationRepository,
                                   UserRepository userRepository,
                                   ZoneRepository zoneRepository,
                                   NotificationService notificationService) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.zoneRepository = zoneRepository;
        this.notificationService = notificationService;
    }

    // ── Get paginated notifications ───────────────────────────────────

    @Operation(summary = "Get paginated notifications for current user")
    @GetMapping
    public ResponseEntity<Page<Notification>> getMyNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        User user = getUser(authentication);
        List<Notification> all = notificationRepository.findByUserOrderByCreatedAtDesc(user);
        int start = Math.min(page * size, all.size());
        int end   = Math.min(start + size, all.size());
        return ResponseEntity.ok(new PageImpl<>(all.subList(start, end),
                PageRequest.of(page, size), all.size()));
    }

    // ── Unread ────────────────────────────────────────────────────────

    @Operation(summary = "Get unread notifications")
    @GetMapping("/unread")
    public ResponseEntity<List<Notification>> getUnread(Authentication authentication) {
        return ResponseEntity.ok(
                notificationRepository.findByUserAndIsReadFalseOrderByCreatedAtDesc(getUser(authentication)));
    }

    @Operation(summary = "Get unread notification count")
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication authentication) {
        long count = notificationService.countUnread(getUser(authentication));
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    // ── Mark read ─────────────────────────────────────────────────────

    @Operation(summary = "Mark a notification as read")
    @PutMapping("/{id}/read")
    public ResponseEntity<String> markRead(@PathVariable Long id, Authentication authentication) {
        User user = getUser(authentication);
        Notification n = findNotification(id);
        if (!n.getUser().getId().equals(user.getId())) throw new UnauthorizedAccessException("Access denied.");
        n.setIsRead(true);
        notificationRepository.save(n);
        return ResponseEntity.ok("Marked as read.");
    }

    @Operation(summary = "Mark all notifications as read")
    @PutMapping("/mark-all-read")
    public ResponseEntity<String> markAllRead(Authentication authentication) {
        notificationRepository.markAllReadByUser(getUser(authentication));
        return ResponseEntity.ok("All notifications marked as read.");
    }

    // ── Delete ────────────────────────────────────────────────────────

    @Operation(summary = "Delete a notification")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id, Authentication authentication) {
        User user = getUser(authentication);
        Notification n = findNotification(id);
        if (!n.getUser().getId().equals(user.getId())) throw new UnauthorizedAccessException("Access denied.");
        notificationRepository.delete(n);
        return ResponseEntity.ok("Notification deleted.");
    }

    // ── Send (Admin / Community Manager) ─────────────────────────────

    @Operation(summary = "Send notification to a user (Admin/Manager)")
    @PostMapping("/send")
    public ResponseEntity<String> send(@RequestBody SendRequest request) {
        User target = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.userId()));
        notificationService.notifyUser(target, request.title(), request.message(),
                request.priority() != null ? request.priority() : Notification.Priority.MEDIUM, true);
        return ResponseEntity.ok("Notification sent.");
    }

    @Operation(summary = "Broadcast announcement to all residents in a zone (Community Manager)")
    @PostMapping("/broadcast")
    public ResponseEntity<String> broadcast(@RequestBody BroadcastRequest request) {
        Zone zone = zoneRepository.findById(request.zoneId())
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found: " + request.zoneId()));
        notificationService.broadcastToZone(zone, request.title(), request.message(),
                request.priority() != null ? request.priority() : Notification.Priority.MEDIUM);
        return ResponseEntity.ok("Broadcast sent to zone: " + zone.getName());
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private User getUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

    private Notification findNotification(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + id));
    }

    public record SendRequest(@NotNull Long userId, @NotBlank String title,
                               String message, Notification.Priority priority) {}

    public record BroadcastRequest(@NotNull Long zoneId, @NotBlank String title,
                                    String message, Notification.Priority priority) {}
}
