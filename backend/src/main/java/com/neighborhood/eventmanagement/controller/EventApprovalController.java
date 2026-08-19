package com.neighborhood.eventmanagement.controller;

import com.neighborhood.eventmanagement.entity.*;
import com.neighborhood.eventmanagement.exception.ResourceNotFoundException;
import com.neighborhood.eventmanagement.exception.UnauthorizedAccessException;
import com.neighborhood.eventmanagement.exception.ValidationException;
import com.neighborhood.eventmanagement.repository.EventApprovalRepository;
import com.neighborhood.eventmanagement.repository.EventRepository;
import com.neighborhood.eventmanagement.repository.UserRepository;
import com.neighborhood.eventmanagement.service.NotificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@Tag(name = "Event Approval", description = "Approval workflow (SRS FR6, FR12, 8.5)")
public class EventApprovalController {

    private final EventRepository eventRepository;
    private final EventApprovalRepository approvalRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public EventApprovalController(EventRepository eventRepository,
                                   EventApprovalRepository approvalRepository,
                                   UserRepository userRepository,
                                   NotificationService notificationService) {
        this.eventRepository = eventRepository;
        this.approvalRepository = approvalRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    // ── Submit for approval (DRAFT → PENDING_APPROVAL) ────────────────

    @Operation(summary = "Submit event for approval (DRAFT → PENDING_APPROVAL)")
    @PostMapping("/api/events/{eventId}/submit-for-approval")
    public ResponseEntity<String> submit(@PathVariable Long eventId) {
        Event event = getEvent(eventId);
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new ValidationException("Only DRAFT events can be submitted for approval.");
        }
        event.setStatus(EventStatus.PENDING_APPROVAL);
        eventRepository.save(event);

        EventApproval approval = approvalRepository.findByEvent(event).orElseGet(EventApproval::new);
        approval.setEvent(event);
        approval.setStatus(EventApproval.ApprovalStatus.PENDING);
        approval.setSubmittedAt(LocalDateTime.now());
        approval.setApprovedAt(null);
        approvalRepository.save(approval);

        return ResponseEntity.ok("Event submitted for approval.");
    }

    // ── Pending list ──────────────────────────────────────────────────

    @Operation(summary = "Get all pending approvals (zone-filtered for ZONE_COORDINATOR)")
    @GetMapping("/api/approvals/pending")
    public ResponseEntity<List<EventApproval>> getPending(Authentication authentication) {
        User actor = getUser(authentication);
        if (actor.getRole() == Role.ZONE_COORDINATOR && actor.getZone() != null) {
            return ResponseEntity.ok(approvalRepository.findPendingByZone(actor.getZone().getId()));
        }
        return ResponseEntity.ok(approvalRepository.findByStatus(EventApproval.ApprovalStatus.PENDING));
    }

    // ── History ───────────────────────────────────────────────────────

    @Operation(summary = "Get approval history (all decided approvals)")
    @GetMapping("/api/approvals/history")
    public ResponseEntity<List<EventApproval>> getHistory() {
        return ResponseEntity.ok(approvalRepository.findHistory());
    }

    // ── Update approval status ────────────────────────────────────────

    @Operation(summary = "Approve, reject, or request revision on an approval")
    @PutMapping("/api/approvals/{approvalId}")
    public ResponseEntity<String> updateApproval(@PathVariable Long approvalId,
                                                 @RequestParam EventApproval.ApprovalStatus status,
                                                 @RequestParam(required = false) String remarks,
                                                 Authentication authentication) {
        User actor = getUser(authentication);
        EventApproval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new ResourceNotFoundException("Approval not found: " + approvalId));

        if (approval.getStatus() != EventApproval.ApprovalStatus.PENDING) {
            throw new ValidationException("Only PENDING approvals can be updated.");
        }

        enforceZoneAccess(actor, approval.getEvent());

        LocalDateTime now = LocalDateTime.now();
        approval.setApprovedBy(actor);
        approval.setStatus(status);
        approval.setRemarks(remarks);
        approval.setApprovedAt(now);
        approvalRepository.save(approval);

        Event event = approval.getEvent();
        if (status == EventApproval.ApprovalStatus.APPROVED) {
            event.setStatus(EventStatus.APPROVED);
            event.setApprovedBy(actor);
            event.setApprovalDate(now);
        } else if (status == EventApproval.ApprovalStatus.REJECTED) {
            event.setStatus(EventStatus.REJECTED);
        }
        // NEEDS_REVISION keeps event in PENDING_APPROVAL — organizer must resubmit
        eventRepository.save(event);

        if (event.getOrganizer() != null) {
            notificationService.notifyEventApproval(event.getOrganizer(), event.getTitle(),
                    status == EventApproval.ApprovalStatus.APPROVED);
        }

        return ResponseEntity.ok("Approval updated to " + status);
    }

    // ── Add comment ───────────────────────────────────────────────────

    @Operation(summary = "Add reviewer comment to an approval")
    @PostMapping("/api/approvals/{approvalId}/comments")
    public ResponseEntity<String> addComment(@PathVariable Long approvalId,
                                             @RequestParam String comment,
                                             Authentication authentication) {
        User actor = getUser(authentication);
        EventApproval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new ResourceNotFoundException("Approval not found: " + approvalId));

        enforceZoneAccess(actor, approval.getEvent());

        String existing = approval.getRemarks() != null ? approval.getRemarks() + "\n" : "";
        approval.setRemarks(existing + comment);
        approvalRepository.save(approval);
        return ResponseEntity.ok("Comment added.");
    }

    // ── Legacy manage endpoints ───────────────────────────────────────

    @Operation(summary = "Approve event (shortcut)")
    @PostMapping("/api/events/manage/{eventId}/approve")
    public ResponseEntity<String> approve(@PathVariable Long eventId,
                                          @RequestParam(required = false) String remarks,
                                          Authentication authentication) {
        User actor = getUser(authentication);
        Event event = getEvent(eventId);
        if (event.getStatus() != EventStatus.PENDING_APPROVAL) {
            throw new ValidationException("Event is not pending approval.");
        }
        enforceZoneAccess(actor, event);

        LocalDateTime now = LocalDateTime.now();
        event.setStatus(EventStatus.APPROVED);
        event.setApprovedBy(actor);
        event.setApprovalDate(now);
        eventRepository.save(event);

        saveApproval(event, actor, EventApproval.ApprovalStatus.APPROVED, remarks, now);
        if (event.getOrganizer() != null)
            notificationService.notifyEventApproval(event.getOrganizer(), event.getTitle(), true);
        return ResponseEntity.ok("Event approved.");
    }

    @Operation(summary = "Reject event (shortcut)")
    @PostMapping("/api/events/manage/{eventId}/reject")
    public ResponseEntity<String> reject(@PathVariable Long eventId,
                                         @RequestParam(required = false) String remarks,
                                         Authentication authentication) {
        User actor = getUser(authentication);
        Event event = getEvent(eventId);
        if (event.getStatus() != EventStatus.PENDING_APPROVAL) {
            throw new ValidationException("Event is not pending approval.");
        }
        enforceZoneAccess(actor, event);

        event.setStatus(EventStatus.REJECTED);
        eventRepository.save(event);

        saveApproval(event, actor, EventApproval.ApprovalStatus.REJECTED, remarks, LocalDateTime.now());
        if (event.getOrganizer() != null)
            notificationService.notifyEventApproval(event.getOrganizer(), event.getTitle(), false);
        return ResponseEntity.ok("Event rejected.");
    }

    @Operation(summary = "Submit event for approval (legacy path)")
    @PostMapping("/api/events/manage/{eventId}/submit")
    public ResponseEntity<String> submitLegacy(@PathVariable Long eventId) {
        return submit(eventId);
    }

    @Operation(summary = "Publish an approved event")
    @PostMapping("/api/events/manage/{eventId}/publish")
    public ResponseEntity<String> publish(@PathVariable Long eventId) {
        Event event = getEvent(eventId);
        if (event.getStatus() != EventStatus.APPROVED) {
            throw new ValidationException("Only APPROVED events can be published.");
        }
        event.setStatus(EventStatus.PUBLISHED);
        eventRepository.save(event);
        return ResponseEntity.ok("Event published.");
    }

    @Operation(summary = "Cancel an event")
    @PostMapping("/api/events/manage/{eventId}/cancel")
    public ResponseEntity<String> cancel(@PathVariable Long eventId) {
        Event event = getEvent(eventId);
        event.setStatus(EventStatus.CANCELLED);
        eventRepository.save(event);
        return ResponseEntity.ok("Event cancelled.");
    }

    @Operation(summary = "Get pending events (legacy path)")
    @GetMapping("/api/events/manage/pending")
    public ResponseEntity<List<Event>> getPendingEvents() {
        return ResponseEntity.ok(
                eventRepository.findAll().stream()
                        .filter(e -> e.getStatus() == EventStatus.PENDING_APPROVAL)
                        .toList());
    }

    // ── Helpers ───────────────────────────────────────────────────────

    /**
     * ZONE_COORDINATOR can only act on events in their own zone.
     * Zone is resolved from: event.venue.zone → event.zone (fallback).
     * If neither is set, ZONE_COORDINATOR is denied (cannot approve zone-less events).
     */
    private void enforceZoneAccess(User actor, Event event) {
        if (actor.getRole() != Role.ZONE_COORDINATOR) return;

        Zone eventZone = null;
        if (event.getVenue() != null && event.getVenue().getZone() != null) {
            eventZone = event.getVenue().getZone();
        } else if (event.getZone() != null) {
            eventZone = event.getZone();
        }

        if (eventZone == null) {
            throw new UnauthorizedAccessException(
                    "Zone Coordinators cannot approve events with no assigned zone.");
        }
        if (actor.getZone() == null || !actor.getZone().getId().equals(eventZone.getId())) {
            throw new UnauthorizedAccessException(
                    "Zone Coordinators can only approve events in their own zone.");
        }
    }

    private void saveApproval(Event event, User actor, EventApproval.ApprovalStatus status,
                               String remarks, LocalDateTime decidedAt) {
        EventApproval approval = approvalRepository.findByEvent(event).orElseGet(EventApproval::new);
        approval.setEvent(event);
        approval.setApprovedBy(actor);
        approval.setStatus(status);
        approval.setRemarks(remarks);
        approval.setApprovedAt(decidedAt);
        if (approval.getSubmittedAt() == null) approval.setSubmittedAt(decidedAt);
        approvalRepository.save(approval);
    }

    private Event getEvent(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + id));
    }

    private User getUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }
}
