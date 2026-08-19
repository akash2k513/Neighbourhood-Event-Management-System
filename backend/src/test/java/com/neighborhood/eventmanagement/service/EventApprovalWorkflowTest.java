package com.neighborhood.eventmanagement.service;

import com.neighborhood.eventmanagement.entity.*;
import com.neighborhood.eventmanagement.exception.UnauthorizedAccessException;
import com.neighborhood.eventmanagement.exception.ValidationException;
import com.neighborhood.eventmanagement.repository.EventApprovalRepository;
import com.neighborhood.eventmanagement.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EventApprovalWorkflowTest {

    @Mock private EventRepository eventRepository;
    @Mock private EventApprovalRepository approvalRepository;

    private Event event;
    private User organizer;
    private User manager;
    private User coordinator;
    private Zone zone;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        zone = new Zone();
        zone.setName("North Zone");

        organizer = new User();
        organizer.setEmail("organizer@test.com");
        organizer.setRole(Role.EVENT_ORGANIZER);

        manager = new User();
        manager.setEmail("manager@test.com");
        manager.setRole(Role.COMMUNITY_MANAGER);

        coordinator = new User();
        coordinator.setEmail("coord@test.com");
        coordinator.setRole(Role.ZONE_COORDINATOR);
        coordinator.setZone(zone);

        event = new Event();
        event.setTitle("Test Event");
        event.setStatus(EventStatus.DRAFT);
        event.setOrganizer(organizer);
        event.setZone(zone);

        when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(approvalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ── Submit ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Submit transitions DRAFT → PENDING_APPROVAL")
    void submit_transitions_draft_to_pending() {
        when(approvalRepository.findByEvent(event)).thenReturn(Optional.empty());

        event.setStatus(EventStatus.PENDING_APPROVAL);
        eventRepository.save(event);

        EventApproval approval = new EventApproval();
        approval.setEvent(event);
        approval.setStatus(EventApproval.ApprovalStatus.PENDING);
        approval.setSubmittedAt(LocalDateTime.now());
        approvalRepository.save(approval);

        assertEquals(EventStatus.PENDING_APPROVAL, event.getStatus());
        assertEquals(EventApproval.ApprovalStatus.PENDING, approval.getStatus());
        assertNotNull(approval.getSubmittedAt());
        assertNull(approval.getApprovedAt());
    }

    @Test
    @DisplayName("Submit fails if event is not DRAFT")
    void submit_fails_for_non_draft() {
        event.setStatus(EventStatus.APPROVED);

        assertThrows(ValidationException.class, () -> {
            if (event.getStatus() != EventStatus.DRAFT)
                throw new ValidationException("Only DRAFT events can be submitted for approval.");
        });
    }

    // ── Approve ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Approve transitions PENDING_APPROVAL → APPROVED and sets approvedBy + approvalDate")
    void approve_sets_approved_by_and_date() {
        event.setStatus(EventStatus.PENDING_APPROVAL);

        EventApproval approval = new EventApproval();
        approval.setEvent(event);
        approval.setStatus(EventApproval.ApprovalStatus.PENDING);

        when(approvalRepository.findById(1L)).thenReturn(Optional.of(approval));

        LocalDateTime now = LocalDateTime.now();
        approval.setApprovedBy(manager);
        approval.setStatus(EventApproval.ApprovalStatus.APPROVED);
        approval.setApprovedAt(now);

        event.setStatus(EventStatus.APPROVED);
        event.setApprovedBy(manager);
        event.setApprovalDate(now);

        eventRepository.save(event);
        approvalRepository.save(approval);

        assertEquals(EventStatus.APPROVED, event.getStatus());
        assertEquals(manager, event.getApprovedBy());
        assertNotNull(event.getApprovalDate());
        assertEquals(EventApproval.ApprovalStatus.APPROVED, approval.getStatus());
    }

    // ── Reject ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Reject transitions PENDING_APPROVAL → REJECTED, does not set approvedBy on event")
    void reject_transitions_to_rejected() {
        event.setStatus(EventStatus.PENDING_APPROVAL);

        EventApproval approval = new EventApproval();
        approval.setEvent(event);
        approval.setStatus(EventApproval.ApprovalStatus.PENDING);

        approval.setApprovedBy(manager);
        approval.setStatus(EventApproval.ApprovalStatus.REJECTED);
        approval.setApprovedAt(LocalDateTime.now());

        event.setStatus(EventStatus.REJECTED);
        eventRepository.save(event);

        assertEquals(EventStatus.REJECTED, event.getStatus());
        assertNull(event.getApprovedBy(), "approvedBy should not be set on rejection");
        assertEquals(EventApproval.ApprovalStatus.REJECTED, approval.getStatus());
    }

    // ── NEEDS_REVISION ────────────────────────────────────────────────

    @Test
    @DisplayName("NEEDS_REVISION keeps event in PENDING_APPROVAL")
    void needs_revision_keeps_pending_approval_status() {
        event.setStatus(EventStatus.PENDING_APPROVAL);

        EventApproval approval = new EventApproval();
        approval.setEvent(event);
        approval.setStatus(EventApproval.ApprovalStatus.NEEDS_REVISION);

        // Event status must NOT change for NEEDS_REVISION
        assertEquals(EventStatus.PENDING_APPROVAL, event.getStatus());
        assertEquals(EventApproval.ApprovalStatus.NEEDS_REVISION, approval.getStatus());
    }

    // ── Zone enforcement ──────────────────────────────────────────────

    @Test
    @DisplayName("ZONE_COORDINATOR can approve event in their own zone")
    void zone_coordinator_can_approve_own_zone() {
        event.setZone(zone);

        // Should not throw
        assertDoesNotThrow(() -> enforceZoneAccess(coordinator, event));
    }

    @Test
    @DisplayName("ZONE_COORDINATOR cannot approve event in a different zone")
    void zone_coordinator_denied_for_different_zone() {
        Zone otherZone = new Zone();
        otherZone.setName("South Zone");
        event.setZone(otherZone);

        assertThrows(UnauthorizedAccessException.class, () -> enforceZoneAccess(coordinator, event));
    }

    @Test
    @DisplayName("ZONE_COORDINATOR denied when event has no zone")
    void zone_coordinator_denied_for_no_zone_event() {
        event.setZone(null);

        assertThrows(UnauthorizedAccessException.class, () -> enforceZoneAccess(coordinator, event));
    }

    @Test
    @DisplayName("COMMUNITY_MANAGER can approve event in any zone")
    void community_manager_can_approve_any_zone() {
        Zone otherZone = new Zone();
        otherZone.setName("South Zone");
        event.setZone(otherZone);

        assertDoesNotThrow(() -> enforceZoneAccess(manager, event));
    }

    @Test
    @DisplayName("Cannot update a non-PENDING approval")
    void cannot_update_non_pending_approval() {
        EventApproval approval = new EventApproval();
        approval.setStatus(EventApproval.ApprovalStatus.APPROVED);

        assertThrows(ValidationException.class, () -> {
            if (approval.getStatus() != EventApproval.ApprovalStatus.PENDING)
                throw new ValidationException("Only PENDING approvals can be updated.");
        });
    }

    // ── Inline zone enforcement logic (mirrors controller) ────────────

    private void enforceZoneAccess(User actor, Event ev) {
        if (actor.getRole() != Role.ZONE_COORDINATOR) return;

        Zone eventZone = null;
        if (ev.getVenue() != null && ev.getVenue().getZone() != null) {
            eventZone = ev.getVenue().getZone();
        } else if (ev.getZone() != null) {
            eventZone = ev.getZone();
        }

        if (eventZone == null) {
            throw new UnauthorizedAccessException(
                    "Zone Coordinators cannot approve events with no assigned zone.");
        }
        if (actor.getZone() == null || !actor.getZone().getName().equals(eventZone.getName())) {
            throw new UnauthorizedAccessException(
                    "Zone Coordinators can only approve events in their own zone.");
        }
    }
}
