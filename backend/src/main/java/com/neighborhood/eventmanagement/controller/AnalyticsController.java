package com.neighborhood.eventmanagement.controller;

import com.neighborhood.eventmanagement.entity.EventStatus;
import com.neighborhood.eventmanagement.repository.EventRegistrationRepository;
import com.neighborhood.eventmanagement.repository.EventRepository;
import com.neighborhood.eventmanagement.repository.UserRepository;
import com.neighborhood.eventmanagement.repository.VenueRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@Tag(name = "Analytics & Reports", description = "Analytics and reporting endpoints (SRS FR9, FR10, 11.1.2)")
public class AnalyticsController {

    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final VenueRepository venueRepository;

    public AnalyticsController(EventRepository eventRepository,
                                EventRegistrationRepository registrationRepository,
                                UserRepository userRepository,
                                VenueRepository venueRepository) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.userRepository = userRepository;
        this.venueRepository = venueRepository;
    }

    // ── Event summary ─────────────────────────────────────────────────

    @Operation(summary = "Get event summary statistics")
    @GetMapping("/api/analytics/events/summary")
    public ResponseEntity<Map<String, Object>> getEventSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalEvents", eventRepository.count());
        summary.put("publishedEvents", eventRepository.findAll().stream()
                .filter(e -> e.getStatus() == EventStatus.PUBLISHED).count());
        summary.put("pendingApproval", eventRepository.findAll().stream()
                .filter(e -> e.getStatus() == EventStatus.PENDING_APPROVAL).count());
        summary.put("cancelledEvents", eventRepository.findAll().stream()
                .filter(e -> e.getStatus() == EventStatus.CANCELLED).count());
        summary.put("totalRegistrations", registrationRepository.count());
        return ResponseEntity.ok(summary);
    }

    // ── Events by category ────────────────────────────────────────────

    @Operation(summary = "Get event analytics grouped by category")
    @GetMapping("/api/analytics/events/by-category")
    public ResponseEntity<Map<String, Long>> getEventsByCategory() {
        Map<String, Long> result = new HashMap<>();
        eventRepository.findAll().forEach(e ->
                result.merge(e.getCategory().name(), 1L, Long::sum));
        return ResponseEntity.ok(result);
    }

    // ── Attendance ────────────────────────────────────────────────────

    @Operation(summary = "Get attendance statistics")
    @GetMapping("/api/analytics/events/attendance")
    public ResponseEntity<Map<String, Object>> getAttendance() {
        Map<String, Object> stats = new HashMap<>();
        long attended = registrationRepository.findAll().stream()
                .filter(r -> r.getStatus().name().equals("ATTENDED")).count();
        long registered = registrationRepository.findAll().stream()
                .filter(r -> r.getStatus().name().equals("REGISTERED")).count();
        long waitlisted = registrationRepository.findAll().stream()
                .filter(r -> r.getStatus().name().equals("WAITLISTED")).count();
        stats.put("attended", attended);
        stats.put("registered", registered);
        stats.put("waitlisted", waitlisted);
        return ResponseEntity.ok(stats);
    }

    // ── Community engagement ──────────────────────────────────────────

    @Operation(summary = "Get community engagement statistics")
    @GetMapping("/api/analytics/community/engagement")
    public ResponseEntity<Map<String, Object>> getCommunityEngagement() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalEvents", eventRepository.count());
        stats.put("totalRegistrations", registrationRepository.count());
        return ResponseEntity.ok(stats);
    }

    // ── Venue utilization ─────────────────────────────────────────────

    @Operation(summary = "Get venue utilization statistics")
    @GetMapping("/api/analytics/venues/utilization")
    public ResponseEntity<Map<String, Object>> getVenueUtilization() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalVenues", venueRepository.count());
        stats.put("availableVenues", venueRepository.findByIsAvailableTrue().size());
        return ResponseEntity.ok(stats);
    }

    // ── Reports ───────────────────────────────────────────────────────

    @Operation(summary = "Get events report")
    @GetMapping("/api/reports/events")
    public ResponseEntity<Map<String, Object>> getEventsReport() {
        Map<String, Object> report = new HashMap<>();
        report.put("events", eventRepository.findAll());
        report.put("generatedAt", java.time.LocalDateTime.now());
        return ResponseEntity.ok(report);
    }

    @Operation(summary = "Get community report")
    @GetMapping("/api/reports/community")
    public ResponseEntity<Map<String, Object>> getCommunityReport() {
        Map<String, Object> report = new HashMap<>();
        report.put("users", userRepository.count());
        report.put("registrations", registrationRepository.count());
        report.put("generatedAt", java.time.LocalDateTime.now());
        return ResponseEntity.ok(report);
    }

    @Operation(summary = "Generate custom report")
    @PostMapping("/api/reports/custom")
    public ResponseEntity<Map<String, Object>> getCustomReport(
            @RequestBody Map<String, Object> filters) {
        Map<String, Object> report = new HashMap<>();
        report.put("filters", filters);
        report.put("totalEvents", eventRepository.count());
        report.put("totalUsers", userRepository.count());
        report.put("generatedAt", java.time.LocalDateTime.now());
        return ResponseEntity.ok(report);
    }
}
