package com.neighborhood.eventmanagement.controller;

import com.neighborhood.eventmanagement.entity.Event;
import com.neighborhood.eventmanagement.entity.EventApproval;
import com.neighborhood.eventmanagement.entity.EventStatus;
import com.neighborhood.eventmanagement.entity.User;
import com.neighborhood.eventmanagement.exception.ResourceNotFoundException;
import com.neighborhood.eventmanagement.exception.ValidationException;
import com.neighborhood.eventmanagement.repository.EventApprovalRepository;
import com.neighborhood.eventmanagement.repository.EventRepository;
import com.neighborhood.eventmanagement.repository.UserRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/events/manage")
@Tag(name = "Event Approval", description = "APIs for approving or rejecting events (Community Manager / Admin)")
public class EventApprovalController {

    private final EventRepository eventRepository;
    private final EventApprovalRepository approvalRepository;
    private final UserRepository userRepository;

    public EventApprovalController(EventRepository eventRepository,
                                    EventApprovalRepository approvalRepository,
                                    UserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.approvalRepository = approvalRepository;
        this.userRepository = userRepository;
    }

    @Operation(summary = "Submit event for approval")
    @PostMapping("/{eventId}/submit")
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
        approval.setApprovedAt(LocalDateTime.now());
        approvalRepository.save(approval);

        return ResponseEntity.ok("Event submitted for approval.");
    }

    @Operation(summary = "Approve an event")
    @PostMapping("/{eventId}/approve")
    public ResponseEntity<String> approve(@PathVariable Long eventId,
                                          @RequestParam(required = false) String remarks,
                                          Authentication authentication) {
        User approver = getUser(authentication);
        Event event = getEvent(eventId);

        if (event.getStatus() != EventStatus.PENDING_APPROVAL) {
            throw new ValidationException("Event is not pending approval.");
        }

        event.setStatus(EventStatus.APPROVED);
        eventRepository.save(event);

        EventApproval approval = approvalRepository.findByEvent(event).orElseGet(EventApproval::new);
        approval.setEvent(event);
        approval.setApprovedBy(approver);
        approval.setStatus(EventApproval.ApprovalStatus.APPROVED);
        approval.setRemarks(remarks);
        approval.setApprovedAt(LocalDateTime.now());
        approvalRepository.save(approval);

        return ResponseEntity.ok("Event approved.");
    }

    @Operation(summary = "Reject an event")
    @PostMapping("/{eventId}/reject")
    public ResponseEntity<String> reject(@PathVariable Long eventId,
                                         @RequestParam(required = false) String remarks,
                                         Authentication authentication) {
        User approver = getUser(authentication);
        Event event = getEvent(eventId);

        if (event.getStatus() != EventStatus.PENDING_APPROVAL) {
            throw new ValidationException("Event is not pending approval.");
        }

        event.setStatus(EventStatus.REJECTED);
        eventRepository.save(event);

        EventApproval approval = approvalRepository.findByEvent(event).orElseGet(EventApproval::new);
        approval.setEvent(event);
        approval.setApprovedBy(approver);
        approval.setStatus(EventApproval.ApprovalStatus.REJECTED);
        approval.setRemarks(remarks);
        approval.setApprovedAt(LocalDateTime.now());
        approvalRepository.save(approval);

        return ResponseEntity.ok("Event rejected.");
    }

    @Operation(summary = "Publish an approved event")
    @PostMapping("/{eventId}/publish")
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
    @PostMapping("/{eventId}/cancel")
    public ResponseEntity<String> cancel(@PathVariable Long eventId) {
        Event event = getEvent(eventId);
        event.setStatus(EventStatus.CANCELLED);
        eventRepository.save(event);
        return ResponseEntity.ok("Event cancelled.");
    }

    @Operation(summary = "Get all pending approval events")
    @GetMapping("/pending")
    public ResponseEntity<List<Event>> getPendingEvents() {
        return ResponseEntity.ok(
                eventRepository.findAll().stream()
                        .filter(e -> e.getStatus() == EventStatus.PENDING_APPROVAL)
                        .toList()
        );
    }

    private Event getEvent(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));
    }

    private User getUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }
}
