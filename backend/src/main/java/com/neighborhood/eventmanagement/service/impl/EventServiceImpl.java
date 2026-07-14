package com.neighborhood.eventmanagement.service.impl;

import com.neighborhood.eventmanagement.audit.Auditable;
import com.neighborhood.eventmanagement.dto.EventRequest;
import com.neighborhood.eventmanagement.dto.EventResponse;
import com.neighborhood.eventmanagement.entity.Event;
import com.neighborhood.eventmanagement.entity.EventCategory;
import com.neighborhood.eventmanagement.entity.EventStatus;
import com.neighborhood.eventmanagement.entity.User;
import com.neighborhood.eventmanagement.entity.Zone;
import com.neighborhood.eventmanagement.exception.ResourceNotFoundException;
import com.neighborhood.eventmanagement.exception.ValidationException;
import com.neighborhood.eventmanagement.repository.EventRepository;
import com.neighborhood.eventmanagement.repository.UserRepository;
import com.neighborhood.eventmanagement.repository.ZoneRepository;
import com.neighborhood.eventmanagement.service.EventService;
import com.neighborhood.eventmanagement.specification.EventSpecification;
import com.neighborhood.eventmanagement.util.EventMapper;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final ZoneRepository zoneRepository;

    public EventServiceImpl(
            EventRepository eventRepository,
            UserRepository userRepository,
            ZoneRepository zoneRepository) {

        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.zoneRepository = zoneRepository;
    }

    // =====================================================
    // CREATE EVENT
    // =====================================================

    @Override
    @Auditable(action = "CREATE_EVENT")
    public EventResponse createEvent(EventRequest request) {

        validateEvent(request);

        User organizer = userRepository.findById(request.getOrganizerId())
                .orElseThrow(() -> new ResourceNotFoundException("Organizer not found."));

        Zone zone = zoneRepository.findById(request.getZoneId())
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found."));

        Event event = EventMapper.toEntity(request, organizer, zone);
        event.setStatus(EventStatus.DRAFT);
        event.setRegisteredCount(0);

        return EventMapper.toResponse(eventRepository.save(event));
    }

    // =====================================================
    // UPDATE EVENT
    // =====================================================

    @Override
    @Auditable(action = "UPDATE_EVENT")
    public EventResponse updateEvent(Long id, EventRequest request) {

        validateEvent(request);

        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + id));

        User organizer = userRepository.findById(request.getOrganizerId())
                .orElseThrow(() -> new ResourceNotFoundException("Organizer not found."));

        Zone zone = zoneRepository.findById(request.getZoneId())
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found."));

        // Reject capacity reduction below current registrations
        if (request.getCapacity() < event.getRegisteredCount()) {
            throw new ValidationException(
                    "New capacity (" + request.getCapacity() + ") cannot be less than current registrations ("
                            + event.getRegisteredCount() + ").");
        }

        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setCategory(request.getCategory());
        event.setStartTime(request.getStartTime());
        event.setEndTime(request.getEndTime());
        event.setCapacity(request.getCapacity());
        event.setLocation(request.getLocation());
        event.setRecurring(request.getRecurring());
        event.setRecurrenceType(request.getRecurrenceType());
        event.setRecurrenceEndDate(request.getRecurrenceEndDate());
        event.setOrganizer(organizer);
        event.setZone(zone);

        return EventMapper.toResponse(eventRepository.save(event));
    }

    // =====================================================
    // DELETE EVENT
    // =====================================================

    @Override
    @Auditable(action = "DELETE_EVENT")
    public void deleteEvent(Long id) {

        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + id));

        eventRepository.delete(event);
    }

    // =====================================================
    // GET EVENT BY ID
    // =====================================================

    @Override
    @Transactional(readOnly = true)
    public EventResponse getEventById(Long id) {

        return EventMapper.toResponse(
                eventRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + id)));
    }

    // =====================================================
    // GET ALL EVENTS (paginated + filtered)
    // =====================================================

    @Override
    @Transactional(readOnly = true)
    public Page<EventResponse> getEvents(
            int page, int size, String sortBy, String sortDirection,
            EventCategory category, EventStatus status, Long zoneId) {

        Sort sort = sortDirection.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Event> spec = Specification
                .where(EventSpecification.hasCategory(category))
                .and(EventSpecification.hasStatus(status))
                .and(EventSpecification.hasZoneId(zoneId));

        return eventRepository.findAll(spec, pageable).map(EventMapper::toResponse);
    }

    // =====================================================
    // MY EVENTS (by organizer)
    // =====================================================

    @Override
    @Transactional(readOnly = true)
    public List<EventResponse> getMyEvents(Long organizerId) {

        User organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new ResourceNotFoundException("Organizer not found."));

        return eventRepository.findByOrganizer(organizer)
                .stream().map(EventMapper::toResponse).toList();
    }

    // =====================================================
    // UPCOMING EVENTS
    // =====================================================

    @Override
    @Transactional(readOnly = true)
    public List<EventResponse> getUpcomingEvents() {

        return eventRepository.findUpcomingEventsByStatus(EventStatus.APPROVED, LocalDateTime.now())
                .stream().map(EventMapper::toResponse).toList();
    }

    // =====================================================
    // EVENTS BY CATEGORY
    // =====================================================

    @Override
    @Transactional(readOnly = true)
    public List<EventResponse> getEventsByCategory(EventCategory category) {

        return eventRepository.findByCategory(category)
                .stream().map(EventMapper::toResponse).toList();
    }

    // =====================================================
    // CALENDAR EVENTS (zone + date range)
    // =====================================================

    @Override
    @Transactional(readOnly = true)
    public List<EventResponse> getCalendarEvents(Zone zone, LocalDateTime start, LocalDateTime end) {

        return eventRepository.findEventsByZoneAndDateRange(zone, start, end)
                .stream().map(EventMapper::toResponse).toList();
    }

    // =====================================================
    // SEARCH EVENTS (keyword, paginated)
    // =====================================================

    @Override
    @Transactional(readOnly = true)
    public Page<EventResponse> searchEvents(
            String keyword, int page, int size, String sortBy, String sortDirection) {

        Sort sort = sortDirection.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        return eventRepository
                .findAll(EventSpecification.keywordContains(keyword), pageable)
                .map(EventMapper::toResponse);
    }

    // =====================================================
    // VALIDATION (FR4, FR16)
    // =====================================================

    @Override
    public void validateEvent(EventRequest request) {

        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new ValidationException("Event title is required.");
        }

        if (request.getCategory() == null) {
            throw new ValidationException("Event category is required.");
        }

        if (request.getStartTime() == null || request.getEndTime() == null) {
            throw new ValidationException("Start time and end time are required.");
        }

        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new ValidationException("End time must be after start time.");
        }

        if (request.getCapacity() == null || request.getCapacity() <= 0) {
            throw new ValidationException("Capacity must be greater than zero.");
        }

        // Recurring event: recurrenceType and recurrenceEndDate are mandatory
        if (Boolean.TRUE.equals(request.getRecurring())) {
            if (request.getRecurrenceType() == null || request.getRecurrenceType().isBlank()) {
                throw new ValidationException("Recurrence type is required for recurring events.");
            }
            if (request.getRecurrenceEndDate() == null) {
                throw new ValidationException("Recurrence end date is required for recurring events.");
            }
            if (!request.getRecurrenceEndDate().isAfter(request.getStartTime().toLocalDate())) {
                throw new ValidationException("Recurrence end date must be after the event start date.");
            }
        }
    }
}
