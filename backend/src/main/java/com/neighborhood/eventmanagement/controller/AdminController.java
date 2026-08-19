package com.neighborhood.eventmanagement.controller;

import com.neighborhood.eventmanagement.dto.UserProfileResponse;
import com.neighborhood.eventmanagement.entity.AuditLog;
import com.neighborhood.eventmanagement.entity.User;
import com.neighborhood.eventmanagement.exception.ResourceNotFoundException;
import com.neighborhood.eventmanagement.repository.AuditLogRepository;
import com.neighborhood.eventmanagement.repository.EventRepository;
import com.neighborhood.eventmanagement.repository.UserRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin", description = "Admin-only management APIs (SRS FR13, FR15, 8.11)")
public class AdminController {

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final EventRepository eventRepository;

    // In-memory settings store (production would use DB/config)
    private final Map<String, String> systemSettings = new ConcurrentHashMap<>(Map.of(
            "maxEventsPerPage", "20",
            "registrationEnabled", "true",
            "maintenanceMode", "false"
    ));

    public AdminController(UserRepository userRepository,
                           AuditLogRepository auditLogRepository,
                           EventRepository eventRepository) {
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.eventRepository = eventRepository;
    }

    // ── Dashboard ─────────────────────────────────────────────────────

    @Operation(summary = "Admin dashboard")
    @GetMapping("/dashboard")
    public ResponseEntity<String> dashboard(Authentication authentication) {
        return ResponseEntity.ok("Welcome Admin " + authentication.getName());
    }

    // ── System health ─────────────────────────────────────────────────

    @Operation(summary = "System health check")
    @GetMapping("/system-health")
    public ResponseEntity<Map<String, Object>> systemHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("totalUsers", userRepository.count());
        health.put("totalEvents", eventRepository.count());
        health.put("totalAuditLogs", auditLogRepository.count());
        return ResponseEntity.ok(health);
    }

    // ── Statistics ────────────────────────────────────────────────────

    @Operation(summary = "Get system statistics")
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalEvents", eventRepository.count());
        stats.put("totalAuditLogs", auditLogRepository.count());
        stats.put("generatedAt", LocalDateTime.now());
        return ResponseEntity.ok(stats);
    }

    // ── User management ───────────────────────────────────────────────

    @Operation(summary = "Get all users (password field excluded)")
    @GetMapping("/users")
    public ResponseEntity<List<UserProfileResponse>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll().stream()
                .map(u -> new UserProfileResponse(u.getId(), u.getFullName(), u.getEmail(),
                        u.getRole(), u.getZone() != null ? u.getZone().getId() : null, u.isEnabled()))
                .toList());
    }

    @Operation(summary = "Get user by ID")
    @GetMapping("/users/{id}")
    public ResponseEntity<UserProfileResponse> getUserById(@PathVariable Long id) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        return ResponseEntity.ok(new UserProfileResponse(u.getId(), u.getFullName(), u.getEmail(),
                u.getRole(), u.getZone() != null ? u.getZone().getId() : null, u.isEnabled()));
    }

    @Operation(summary = "Delete a user")
    @DeleteMapping("/users/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        userRepository.delete(userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id)));
        return ResponseEntity.ok("User deleted.");
    }

    @Operation(summary = "Lock a user account")
    @PatchMapping("/users/{id}/lock")
    public ResponseEntity<String> lockUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        user.setAccountLocked(true);
        userRepository.save(user);
        return ResponseEntity.ok("User account locked.");
    }

    @Operation(summary = "Unlock a user account")
    @PatchMapping("/users/{id}/unlock")
    public ResponseEntity<String> unlockUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        user.setAccountLocked(false);
        user.setFailedLoginAttempts(0);
        user.setLockTime(null);
        userRepository.save(user);
        return ResponseEntity.ok("User account unlocked.");
    }

    // ── Audit logs ────────────────────────────────────────────────────

    @Operation(summary = "Get audit logs (filterable by action or userId)")
    @GetMapping("/audit-logs")
    public ResponseEntity<List<AuditLog>> getAuditLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Long userId) {

        if (action != null && !action.isBlank()) {
            return ResponseEntity.ok(auditLogRepository.findByActionContainingIgnoreCase(action));
        }
        if (userId != null) {
            return ResponseEntity.ok(auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId));
        }
        return ResponseEntity.ok(auditLogRepository.findAllByOrderByCreatedAtDesc());
    }

    // ── User activity ─────────────────────────────────────────────────

    @Operation(summary = "Get audit activity for a specific user")
    @GetMapping("/user-activity/{userId}")
    public ResponseEntity<List<AuditLog>> getUserActivity(@PathVariable Long userId) {
        return ResponseEntity.ok(auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId));
    }

    // ── Settings ──────────────────────────────────────────────────────

    @Operation(summary = "Get system settings")
    @GetMapping("/settings")
    public ResponseEntity<Map<String, String>> getSettings() {
        return ResponseEntity.ok(systemSettings);
    }

    @Operation(summary = "Update system settings")
    @PutMapping("/settings")
    public ResponseEntity<Map<String, String>> updateSettings(
            @RequestBody Map<String, String> updates) {
        systemSettings.putAll(updates);
        return ResponseEntity.ok(systemSettings);
    }

    // ── Backup ────────────────────────────────────────────────────────

    @Operation(summary = "Trigger a system backup (returns metadata)")
    @PostMapping("/backup")
    public ResponseEntity<Map<String, Object>> triggerBackup() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "BACKUP_INITIATED");
        result.put("timestamp", LocalDateTime.now());
        result.put("totalUsers", userRepository.count());
        result.put("totalEvents", eventRepository.count());
        result.put("message", "Backup metadata captured. Configure mysqldump for full backup.");
        return ResponseEntity.ok(result);
    }
}
