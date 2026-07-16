package com.neighborhood.eventmanagement.controller;

import com.neighborhood.eventmanagement.entity.Event;
import com.neighborhood.eventmanagement.entity.EventRegistration;
import com.neighborhood.eventmanagement.entity.EventStatus;
import com.neighborhood.eventmanagement.entity.User;
import com.neighborhood.eventmanagement.exception.ResourceNotFoundException;
import com.neighborhood.eventmanagement.exception.ValidationException;
import com.neighborhood.eventmanagement.repository.EventRegistrationRepository;
import com.neighborhood.eventmanagement.repository.EventRepository;
import com.neighborhood.eventmanagement.repository.UserRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/registrations")
@Tag(name = "Event Registrations", description = "APIs for registering and cancelling event attendance")
public class EventRegistrationController {

    private final EventRegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public EventRegistrationController(EventRegistrationRepository registrationRepository,
                                        EventRepository eventRepository,
                                        UserRepository userRepository) {
        this.registrationRepository = registrationRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    @Operation(summary = "Register current user for an event")
    @PostMapping("/events/{eventId}")
    public ResponseEntity<String> register(@PathVariable Long eventId, Authentication authentication) {
        User user = getUser(authentication);
        Event event = getEvent(eventId);

        if (event.getStatus() != EventStatus.PUBLISHED && event.getStatus() != EventStatus.APPROVED) {
            throw new ValidationException("Event is not open for registration.");
        }

        if (registrationRepository.existsByEventAndUser(event, user)) {
            throw new ValidationException("You are already registered for this event.");
        }

        if (event.getRegisteredCount() >= event.getCapacity()) {
            throw new ValidationException("Event is at full capacity.");
        }

        EventRegistration registration = new EventRegistration();
        registration.setEvent(event);
        registration.setUser(user);
        registration.setRegisteredAt(LocalDateTime.now());
        registration.setStatus(EventRegistration.RegistrationStatus.REGISTERED);
        registrationRepository.save(registration);

        event.setRegisteredCount(event.getRegisteredCount() + 1);
        eventRepository.save(event);

        return ResponseEntity.status(HttpStatus.CREATED).body("Successfully registered for the event.");
    }

    @Operation(summary = "Cancel registration for an event")
    @DeleteMapping("/events/{eventId}")
    public ResponseEntity<String> cancel(@PathVariable Long eventId, Authentication authentication) {
        User user = getUser(authentication);
        Event event = getEvent(eventId);

        EventRegistration registration = registrationRepository.findByEventAndUser(event, user)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found."));

        registration.setStatus(EventRegistration.RegistrationStatus.CANCELLED);
        registrationRepository.save(registration);

        event.setRegisteredCount(Math.max(0, event.getRegisteredCount() - 1));
        eventRepository.save(event);

        return ResponseEntity.ok("Registration cancelled.");
    }

    @Operation(summary = "Get all events the current user is registered for")
    @GetMapping("/my")
    public ResponseEntity<List<EventRegistration>> getMyRegistrations(Authentication authentication) {
        User user = getUser(authentication);
        return ResponseEntity.ok(registrationRepository.findByUser(user));
    }

    @Operation(summary = "Get all registrations for an event (Organizer/Admin)")
    @GetMapping("/events/{eventId}")
    public ResponseEntity<List<EventRegistration>> getEventRegistrations(@PathVariable Long eventId) {
        Event event = getEvent(eventId);
        return ResponseEntity.ok(registrationRepository.findByEvent(event));
    }

    private User getUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

    private Event getEvent(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));
    }
}
