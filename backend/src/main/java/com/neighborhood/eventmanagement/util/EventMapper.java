package com.neighborhood.eventmanagement.util;

import com.neighborhood.eventmanagement.dto.EventRequest;
import com.neighborhood.eventmanagement.dto.EventResponse;
import com.neighborhood.eventmanagement.entity.Event;
import com.neighborhood.eventmanagement.entity.User;
import com.neighborhood.eventmanagement.entity.Zone;

public class EventMapper {

    private EventMapper() {
    }

    // =====================================================
    // Request DTO -> Entity
    // =====================================================

    public static Event toEntity(EventRequest request,
            User organizer,
            Zone zone) {

        Event event = new Event();

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

        return event;
    }

    // =====================================================
    // Entity -> Response DTO
    // =====================================================

    public static EventResponse toResponse(Event event) {

        EventResponse response = new EventResponse();

        response.setId(event.getId());

        response.setTitle(event.getTitle());
        response.setDescription(event.getDescription());

        response.setCategory(event.getCategory());
        response.setStatus(event.getStatus());

        response.setStartTime(event.getStartTime());
        response.setEndTime(event.getEndTime());

        response.setCapacity(event.getCapacity());
        response.setRegisteredCount(event.getRegisteredCount());

        response.setLocation(event.getLocation());

        response.setRecurring(event.getRecurring());
        response.setRecurrenceType(event.getRecurrenceType());
        response.setRecurrenceEndDate(event.getRecurrenceEndDate());

        User organizer = event.getOrganizer();

        if (organizer != null) {
            response.setOrganizerName(organizer.getFullName());
            response.setOrganizerId(organizer.getId());
        }

        Zone zone = event.getZone();

        if (zone != null) {
            response.setZoneName(zone.getName());
            response.setZoneId(zone.getId());
        }

        response.setCreatedAt(event.getCreatedAt());
        response.setUpdatedAt(event.getUpdatedAt());

        return response;
    }
}