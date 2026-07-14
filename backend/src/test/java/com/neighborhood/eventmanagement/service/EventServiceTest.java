package com.neighborhood.eventmanagement.service;

import com.neighborhood.eventmanagement.dto.EventRequest;
import com.neighborhood.eventmanagement.dto.EventResponse;
import com.neighborhood.eventmanagement.entity.Event;
import com.neighborhood.eventmanagement.entity.EventCategory;
import com.neighborhood.eventmanagement.entity.EventStatus;
import com.neighborhood.eventmanagement.entity.User;
import com.neighborhood.eventmanagement.entity.Zone;
import com.neighborhood.eventmanagement.exception.ValidationException;
import com.neighborhood.eventmanagement.repository.EventRepository;
import com.neighborhood.eventmanagement.repository.UserRepository;
import com.neighborhood.eventmanagement.repository.ZoneRepository;
import com.neighborhood.eventmanagement.service.impl.EventServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EventServiceTest {

    @Mock private EventRepository eventRepository;
    @Mock private UserRepository userRepository;
    @Mock private ZoneRepository zoneRepository;

    @InjectMocks
    private EventServiceImpl eventService;

    private User organizer;
    private Zone zone;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        organizer = new User();
        organizer.setFullName("Event Organizer");

        zone = new Zone();
        zone.setName("Zone A");
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private EventRequest validRequest() {
        EventRequest r = new EventRequest();
        r.setTitle("Community Meeting");
        r.setDescription("Monthly Meeting");
        r.setCategory(EventCategory.SOCIAL);
        r.setStartTime(LocalDateTime.now().plusDays(1));
        r.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));
        r.setCapacity(100);
        r.setLocation("Community Hall");
        r.setRecurring(false);
        r.setOrganizerId(1L);
        r.setZoneId(1L);
        return r;
    }

    // ------------------------------------------------------------------
    // CREATE EVENT
    // ------------------------------------------------------------------

    @Test
    @DisplayName("createEvent — saves and returns response")
    void createEvent_ShouldSaveSuccessfully() {

        EventRequest request = validRequest();

        Event savedEvent = new Event();
        savedEvent.setTitle(request.getTitle());
        savedEvent.setDescription(request.getDescription());
        savedEvent.setCategory(request.getCategory());
        savedEvent.setStartTime(request.getStartTime());
        savedEvent.setEndTime(request.getEndTime());
        savedEvent.setCapacity(request.getCapacity());
        savedEvent.setLocation(request.getLocation());
        savedEvent.setOrganizer(organizer);
        savedEvent.setZone(zone);
        savedEvent.setStatus(EventStatus.DRAFT);
        savedEvent.setRegisteredCount(0);

        when(userRepository.findById(1L)).thenReturn(Optional.of(organizer));
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone));
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

        EventResponse response = eventService.createEvent(request);

        assertNotNull(response);
        assertEquals("Community Meeting", response.getTitle());
        verify(eventRepository, times(1)).save(any(Event.class));
    }

    // ------------------------------------------------------------------
    // UPDATE EVENT
    // ------------------------------------------------------------------

    @Test
    @DisplayName("updateEvent — updates and returns response")
    void updateEvent_ShouldUpdateSuccessfully() {

        Event existing = new Event();
        existing.setTitle("Old Event");
        existing.setOrganizer(organizer);
        existing.setZone(zone);
        existing.setRegisteredCount(0);

        EventRequest request = validRequest();
        request.setTitle("Updated Event");
        request.setCategory(EventCategory.EDUCATIONAL);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.findById(1L)).thenReturn(Optional.of(organizer));
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        EventResponse response = eventService.updateEvent(1L, request);

        assertNotNull(response);
        assertEquals("Updated Event", response.getTitle());
        verify(eventRepository, times(1)).save(any(Event.class));
    }

    // ------------------------------------------------------------------
    // DELETE EVENT
    // ------------------------------------------------------------------

    @Test
    @DisplayName("deleteEvent — deletes successfully")
    void deleteEvent_ShouldDeleteSuccessfully() {

        Event event = new Event();
        event.setTitle("Delete Test Event");

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        doNothing().when(eventRepository).delete(any(Event.class));

        eventService.deleteEvent(1L);

        verify(eventRepository, times(1)).delete(event);
    }

    // ------------------------------------------------------------------
    // VALIDATION — invalid date range (FR4)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("validateEvent — rejects end time before start time")
    void validateEvent_EndBeforeStart_ThrowsValidationException() {

        EventRequest request = validRequest();
        request.setEndTime(request.getStartTime().minusHours(1)); // end before start

        ValidationException ex = assertThrows(ValidationException.class,
                () -> eventService.validateEvent(request));

        assertTrue(ex.getMessage().contains("End time must be after start time"));
    }

    @Test
    @DisplayName("validateEvent — rejects equal start and end time")
    void validateEvent_EqualStartEnd_ThrowsValidationException() {

        EventRequest request = validRequest();
        request.setEndTime(request.getStartTime()); // same time

        ValidationException ex = assertThrows(ValidationException.class,
                () -> eventService.validateEvent(request));

        assertTrue(ex.getMessage().contains("End time must be after start time"));
    }

    // ------------------------------------------------------------------
    // VALIDATION — over-capacity update (FR16)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("updateEvent — rejects capacity below current registrations")
    void updateEvent_CapacityBelowRegistrations_ThrowsValidationException() {

        Event existing = new Event();
        existing.setTitle("Full Event");
        existing.setOrganizer(organizer);
        existing.setZone(zone);
        existing.setRegisteredCount(50); // 50 already registered

        EventRequest request = validRequest();
        request.setCapacity(30); // trying to reduce below 50

        when(eventRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.findById(1L)).thenReturn(Optional.of(organizer));
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone));

        ValidationException ex = assertThrows(ValidationException.class,
                () -> eventService.updateEvent(1L, request));

        assertTrue(ex.getMessage().contains("cannot be less than current registrations"));
    }

    // ------------------------------------------------------------------
    // VALIDATION — recurring event missing fields
    // ------------------------------------------------------------------

    @Test
    @DisplayName("validateEvent — recurring event without recurrenceType throws exception")
    void validateEvent_RecurringMissingType_ThrowsValidationException() {

        EventRequest request = validRequest();
        request.setRecurring(true);
        request.setRecurrenceType(null);
        request.setRecurrenceEndDate(LocalDate.now().plusMonths(3));

        ValidationException ex = assertThrows(ValidationException.class,
                () -> eventService.validateEvent(request));

        assertTrue(ex.getMessage().contains("Recurrence type is required"));
    }

    @Test
    @DisplayName("validateEvent — recurring event without recurrenceEndDate throws exception")
    void validateEvent_RecurringMissingEndDate_ThrowsValidationException() {

        EventRequest request = validRequest();
        request.setRecurring(true);
        request.setRecurrenceType("WEEKLY");
        request.setRecurrenceEndDate(null);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> eventService.validateEvent(request));

        assertTrue(ex.getMessage().contains("Recurrence end date is required"));
    }

    // ------------------------------------------------------------------
    // VALIDATION — zero capacity
    // ------------------------------------------------------------------

    @Test
    @DisplayName("validateEvent — zero capacity throws exception")
    void validateEvent_ZeroCapacity_ThrowsValidationException() {

        EventRequest request = validRequest();
        request.setCapacity(0);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> eventService.validateEvent(request));

        assertTrue(ex.getMessage().contains("Capacity must be greater than zero"));
    }
}
