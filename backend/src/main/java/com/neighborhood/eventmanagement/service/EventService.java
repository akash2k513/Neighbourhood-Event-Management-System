package com.neighborhood.eventmanagement.service;

import com.neighborhood.eventmanagement.dto.EventRequest;
import com.neighborhood.eventmanagement.dto.EventResponse;
import com.neighborhood.eventmanagement.entity.EventCategory;
import com.neighborhood.eventmanagement.entity.EventStatus;
import com.neighborhood.eventmanagement.entity.Zone;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

public interface EventService {

    // =====================================================
    // CRUD
    // =====================================================

    EventResponse createEvent(EventRequest request);

    EventResponse updateEvent(Long id, EventRequest request);

    void deleteEvent(Long id);

    EventResponse getEventById(Long id);
Page<EventResponse> getEvents(
        int page,
        int size,
        String sortBy,
        String sortDirection,
        EventCategory category,
        EventStatus status,
        Long zoneId
);

    // =====================================================
    // PAGINATION + FILTERING (Issue #4)
    // =====================================================


    // =====================================================
    // SEARCH
    // =====================================================

    Page<EventResponse> searchEvents(
            String keyword,
            int page,
            int size,
            String sortBy,
            String sortDirection
    );

    // =====================================================
    // SRS APIs
    // =====================================================

    List<EventResponse> getMyEvents(Long organizerId);

    List<EventResponse> getUpcomingEvents();

    List<EventResponse> getEventsByCategory(EventCategory category);

    List<EventResponse> getCalendarEvents(
            Zone zone,
            LocalDateTime start,
            LocalDateTime end
    );

    // =====================================================
    // VALIDATION
    // =====================================================

    void validateEvent(EventRequest request);

}