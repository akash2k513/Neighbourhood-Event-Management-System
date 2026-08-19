package com.neighborhood.eventmanagement.controller;

import com.neighborhood.eventmanagement.entity.*;
import com.neighborhood.eventmanagement.exception.ResourceNotFoundException;
import com.neighborhood.eventmanagement.exception.ValidationException;
import com.neighborhood.eventmanagement.repository.*;
import com.neighborhood.eventmanagement.service.NotificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@Tag(name = "Event Registrations", description = "Registration, waitlist, check-in, and feedback (SRS FR5, 8.4)")
public class EventRegistrationController {

    private final EventRegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final FeedbackRepository feedbackRepository;
    private final NotificationService notificationService;

    public EventRegistrationController(EventRegistrationRepository registrationRepository,
                                       EventRepository eventRepository,
                                       UserRepository userRepository,
                                       FeedbackRepository feedbackRepository,
                                       NotificationService notificationService) {
        this.registrationRepository = registrationRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.feedbackRepository = feedbackRepository;
        this.notificationService = notificationService;
    }

    // ── Register ─────────────────────────────────────────────────────

    @Operation(summary = "Register for an event (auto-waitlist when full)")
    @PostMapping("/api/events/{eventId}/register")
    public ResponseEntity<String> register(@PathVariable Long eventId,
                                           Authentication authentication) {
        User user = getUser(authentication);
        Event event = getEvent(eventId);

        if (event.getStatus() != EventStatus.PUBLISHED && event.getStatus() != EventStatus.APPROVED) {
            throw new ValidationException("Event is not open for registration.");
        }
        if (registrationRepository.existsByEventAndUser(event, user)) {
            throw new ValidationException("You are already registered for this event.");
        }

        EventRegistration reg = new EventRegistration();
        reg.setEvent(event);
        reg.setUser(user);
        reg.setRegisteredAt(LocalDateTime.now());

        if (event.getRegisteredCount() >= event.getCapacity()) {
            reg.setStatus(EventRegistration.RegistrationStatus.WAITLISTED);
            registrationRepository.save(reg);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("Event is full. You have been added to the waitlist.");
        }

        reg.setStatus(EventRegistration.RegistrationStatus.REGISTERED);
        registrationRepository.save(reg);
        event.setRegisteredCount(event.getRegisteredCount() + 1);
        eventRepository.save(event);

        notificationService.notifyRegistrationConfirmed(user, event.getTitle());
        return ResponseEntity.status(HttpStatus.CREATED).body("Successfully registered for the event.");
    }

    // ── Unregister (with waitlist promotion) ─────────────────────────

    @Operation(summary = "Unregister from an event (promotes next waitlisted user)")
    @DeleteMapping("/api/events/{eventId}/unregister")
    public ResponseEntity<String> unregister(@PathVariable Long eventId,
                                             Authentication authentication) {
        User user = getUser(authentication);
        Event event = getEvent(eventId);

        EventRegistration reg = registrationRepository.findByEventAndUser(event, user)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found."));

        boolean wasRegistered = reg.getStatus() == EventRegistration.RegistrationStatus.REGISTERED;

        reg.setStatus(EventRegistration.RegistrationStatus.CANCELLED);
        registrationRepository.save(reg);

        if (wasRegistered) {
            event.setRegisteredCount(Math.max(0, event.getRegisteredCount() - 1));
            eventRepository.save(event);
            promoteWaitlisted(event);
        }

        return ResponseEntity.ok("Unregistered successfully.");
    }

    // ── Get registrations ─────────────────────────────────────────────

    @Operation(summary = "Get all registrations for an event (Organizer/Admin)")
    @GetMapping("/api/events/{eventId}/registrations")
    public ResponseEntity<List<EventRegistration>> getRegistrations(@PathVariable Long eventId) {
        return ResponseEntity.ok(registrationRepository.findByEvent(getEvent(eventId)));
    }

    @Operation(summary = "Get attendees (ATTENDED status) for an event")
    @GetMapping("/api/events/{eventId}/attendees")
    public ResponseEntity<List<EventRegistration>> getAttendees(@PathVariable Long eventId) {
        return ResponseEntity.ok(
                registrationRepository.findByEvent(getEvent(eventId)).stream()
                        .filter(r -> r.getStatus() == EventRegistration.RegistrationStatus.ATTENDED)
                        .toList());
    }

    @Operation(summary = "Get my registrations")
    @GetMapping("/api/registrations/my")
    public ResponseEntity<List<EventRegistration>> getMyRegistrations(Authentication authentication) {
        return ResponseEntity.ok(registrationRepository.findByUser(getUser(authentication)));
    }

    // ── Check-in (Issue 10) ───────────────────────────────────────────

    @Operation(summary = "Check in a user to an event")
    @PostMapping("/api/events/{eventId}/check-in/{userId}")
    public ResponseEntity<String> checkIn(@PathVariable Long eventId,
                                          @PathVariable Long userId) {
        Event event = getEvent(eventId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        EventRegistration reg = registrationRepository.findByEventAndUser(event, user)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found."));

        if (reg.getStatus() != EventRegistration.RegistrationStatus.REGISTERED) {
            throw new ValidationException("User is not in REGISTERED status.");
        }

        reg.setStatus(EventRegistration.RegistrationStatus.ATTENDED);
        reg.setCheckInTime(LocalDateTime.now());
        registrationRepository.save(reg);
        return ResponseEntity.ok("Check-in successful.");
    }

    // ── Feedback ──────────────────────────────────────────────────────

    @Operation(summary = "Submit feedback for an event (1-5 rating)")
    @PostMapping("/api/events/{eventId}/feedback")
    public ResponseEntity<String> submitFeedback(@PathVariable Long eventId,
                                                 @RequestBody FeedbackRequest request,
                                                 Authentication authentication) {
        User user = getUser(authentication);
        Event event = getEvent(eventId);

        if (feedbackRepository.existsByEventAndUser(event, user)) {
            throw new ValidationException("You have already submitted feedback for this event.");
        }

        Feedback feedback = new Feedback();
        feedback.setEvent(event);
        feedback.setUser(user);
        feedback.setRating(request.rating());
        feedback.setComment(request.comment());
        feedbackRepository.save(feedback);
        return ResponseEntity.status(HttpStatus.CREATED).body("Feedback submitted.");
    }

    @Operation(summary = "Get feedback for an event")
    @GetMapping("/api/events/{eventId}/feedback")
    public ResponseEntity<List<Feedback>> getFeedback(@PathVariable Long eventId) {
        return ResponseEntity.ok(feedbackRepository.findByEvent(getEvent(eventId)));
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private void promoteWaitlisted(Event event) {
        List<EventRegistration> waitlisted =
                registrationRepository.findWaitlistedByEventOrderByDate(event);
        if (!waitlisted.isEmpty()) {
            EventRegistration next = waitlisted.get(0);
            next.setStatus(EventRegistration.RegistrationStatus.REGISTERED);
            registrationRepository.save(next);
            event.setRegisteredCount(event.getRegisteredCount() + 1);
            eventRepository.save(event);
            notificationService.notifyRegistrationConfirmed(next.getUser(), event.getTitle());
        }
    }

    private User getUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

    private Event getEvent(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + id));
    }

    public record FeedbackRequest(@NotNull @Min(1) @Max(5) Integer rating, String comment) {}
}
