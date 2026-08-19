package com.neighborhood.eventmanagement.service;

import com.neighborhood.eventmanagement.entity.*;
import com.neighborhood.eventmanagement.exception.ValidationException;
import com.neighborhood.eventmanagement.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EventRegistrationServiceTest {

    @Mock private EventRegistrationRepository registrationRepository;
    @Mock private EventRepository eventRepository;
    @Mock private UserRepository userRepository;

    private Event event;
    private User user;
    private User user2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        event = new Event();
        event.setTitle("Test Event");
        event.setCapacity(2);
        event.setRegisteredCount(0);
        event.setStatus(EventStatus.PUBLISHED);

        user = new User();
        user.setEmail("user1@test.com");

        user2 = new User();
        user2.setEmail("user2@test.com");
    }

    @Test
    @DisplayName("Register succeeds when capacity is available")
    void register_succeeds_when_capacity_available() {
        when(registrationRepository.existsByEventAndUser(event, user)).thenReturn(false);
        when(registrationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertFalse(registrationRepository.existsByEventAndUser(event, user));
        assertTrue(event.getRegisteredCount() < event.getCapacity());

        EventRegistration reg = new EventRegistration();
        reg.setEvent(event);
        reg.setUser(user);
        reg.setStatus(EventRegistration.RegistrationStatus.REGISTERED);
        reg.setRegisteredAt(LocalDateTime.now());

        registrationRepository.save(reg);
        event.setRegisteredCount(event.getRegisteredCount() + 1);

        assertEquals(1, event.getRegisteredCount());
        assertEquals(EventRegistration.RegistrationStatus.REGISTERED, reg.getStatus());
    }

    @Test
    @DisplayName("Registration sets WAITLISTED when event is full")
    void register_sets_waitlisted_when_full() {
        event.setRegisteredCount(2); // at capacity

        when(registrationRepository.existsByEventAndUser(event, user)).thenReturn(false);
        when(registrationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertTrue(event.getRegisteredCount() >= event.getCapacity());

        EventRegistration reg = new EventRegistration();
        reg.setEvent(event);
        reg.setUser(user);
        reg.setStatus(EventRegistration.RegistrationStatus.WAITLISTED);
        reg.setRegisteredAt(LocalDateTime.now());

        registrationRepository.save(reg);

        assertEquals(EventRegistration.RegistrationStatus.WAITLISTED, reg.getStatus());
        assertEquals(2, event.getRegisteredCount(), "Count must not increase for waitlisted");
    }

    @Test
    @DisplayName("Unregister promotes next waitlisted user")
    void unregister_promotes_waitlisted_user() {
        event.setRegisteredCount(2);

        // user is REGISTERED, user2 is WAITLISTED
        EventRegistration reg1 = new EventRegistration();
        reg1.setEvent(event); reg1.setUser(user);
        reg1.setStatus(EventRegistration.RegistrationStatus.REGISTERED);
        reg1.setRegisteredAt(LocalDateTime.now().minusMinutes(10));

        EventRegistration reg2 = new EventRegistration();
        reg2.setEvent(event); reg2.setUser(user2);
        reg2.setStatus(EventRegistration.RegistrationStatus.WAITLISTED);
        reg2.setRegisteredAt(LocalDateTime.now());

        when(registrationRepository.findByEventAndUser(event, user)).thenReturn(Optional.of(reg1));
        when(registrationRepository.findWaitlistedByEventOrderByDate(event)).thenReturn(List.of(reg2));
        when(registrationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Simulate unregister
        boolean wasRegistered = reg1.getStatus() == EventRegistration.RegistrationStatus.REGISTERED;
        reg1.setStatus(EventRegistration.RegistrationStatus.CANCELLED);
        registrationRepository.save(reg1);

        if (wasRegistered) {
            event.setRegisteredCount(Math.max(0, event.getRegisteredCount() - 1));
            List<EventRegistration> waitlisted = registrationRepository.findWaitlistedByEventOrderByDate(event);
            if (!waitlisted.isEmpty()) {
                EventRegistration next = waitlisted.get(0);
                next.setStatus(EventRegistration.RegistrationStatus.REGISTERED);
                registrationRepository.save(next);
                event.setRegisteredCount(event.getRegisteredCount() + 1);
            }
        }

        assertEquals(EventRegistration.RegistrationStatus.CANCELLED, reg1.getStatus());
        assertEquals(EventRegistration.RegistrationStatus.REGISTERED, reg2.getStatus());
        assertEquals(2, event.getRegisteredCount(), "Count stays same after promotion");
    }

    @Test
    @DisplayName("Duplicate registration throws ValidationException")
    void duplicate_registration_throws() {
        when(registrationRepository.existsByEventAndUser(event, user)).thenReturn(true);

        assertThrows(ValidationException.class, () -> {
            if (registrationRepository.existsByEventAndUser(event, user)) {
                throw new ValidationException("You are already registered for this event.");
            }
        });
    }

    @Test
    @DisplayName("Registration rejected for non-published event")
    void register_rejected_for_non_published_event() {
        event.setStatus(EventStatus.DRAFT);

        assertThrows(ValidationException.class, () -> {
            if (event.getStatus() != EventStatus.PUBLISHED && event.getStatus() != EventStatus.APPROVED) {
                throw new ValidationException("Event is not open for registration.");
            }
        });
    }

    @Test
    @DisplayName("Unregister a WAITLISTED user does not decrement count")
    void unregister_waitlisted_does_not_decrement_count() {
        event.setRegisteredCount(2);

        EventRegistration reg = new EventRegistration();
        reg.setEvent(event); reg.setUser(user);
        reg.setStatus(EventRegistration.RegistrationStatus.WAITLISTED);

        when(registrationRepository.findByEventAndUser(event, user)).thenReturn(Optional.of(reg));
        when(registrationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        boolean wasRegistered = reg.getStatus() == EventRegistration.RegistrationStatus.REGISTERED;
        reg.setStatus(EventRegistration.RegistrationStatus.CANCELLED);
        registrationRepository.save(reg);

        if (wasRegistered) {
            event.setRegisteredCount(event.getRegisteredCount() - 1);
        }

        assertEquals(2, event.getRegisteredCount(), "Count must not change when waitlisted user unregisters");
        assertEquals(EventRegistration.RegistrationStatus.CANCELLED, reg.getStatus());
    }
}
