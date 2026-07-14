package com.neighborhood.eventmanagement.controller;

import com.neighborhood.eventmanagement.dto.EventRequest;
import com.neighborhood.eventmanagement.dto.EventResponse;
import com.neighborhood.eventmanagement.entity.EventCategory;
import com.neighborhood.eventmanagement.entity.EventStatus;
import com.neighborhood.eventmanagement.entity.Zone;
import com.neighborhood.eventmanagement.exception.ResourceNotFoundException;
import com.neighborhood.eventmanagement.repository.ZoneRepository;
import com.neighborhood.eventmanagement.service.EventService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/events")
@Tag(
        name = "Event Management",
        description = "APIs for creating, updating, deleting, searching, and managing neighborhood events."
)
public class EventController {

    private final EventService eventService;
    private final ZoneRepository zoneRepository;

    public EventController(EventService eventService,
                           ZoneRepository zoneRepository) {

        this.eventService = eventService;
        this.zoneRepository = zoneRepository;
    }

    // =====================================================
    // CREATE EVENT
    // =====================================================

    @Operation(summary = "Create a new event")
    @PostMapping
    public ResponseEntity<EventResponse> createEvent(
            @Valid @RequestBody EventRequest request) {

        EventResponse response = eventService.createEvent(request);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // =====================================================
    // GET ALL EVENTS
    // =====================================================

    @Operation(summary = "Get all events with pagination, sorting and filtering")
    @GetMapping
    public ResponseEntity<Page<EventResponse>> getEvents(

            @Parameter(description = "Page number")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "6") int size,

            @Parameter(description = "Sort field")
            @RequestParam(defaultValue = "startTime") String sortBy,

            @Parameter(description = "Sort direction (asc/desc)")
            @RequestParam(defaultValue = "asc") String sortDirection,

            @Parameter(description = "Filter by category")
            @RequestParam(required = false) EventCategory category,

            @Parameter(description = "Filter by status")
            @RequestParam(required = false) EventStatus status,

            @Parameter(description = "Filter by zone ID")
            @RequestParam(required = false) Long zoneId) {

        return ResponseEntity.ok(

                eventService.getEvents(
                        page,
                        size,
                        sortBy,
                        sortDirection,
                        category,
                        status,
                        zoneId
                )

        );
    }

    // =====================================================
    // GET EVENT BY ID
    // =====================================================

    @Operation(summary = "Get event by ID")
    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEventById(

            @Parameter(description = "Event ID")
            @PathVariable Long id) {

        return ResponseEntity.ok(eventService.getEventById(id));
    }

    // =====================================================
    // UPDATE EVENT
    // =====================================================

    @Operation(summary = "Update an existing event")
    @PutMapping("/{id}")
    public ResponseEntity<EventResponse> updateEvent(

            @Parameter(description = "Event ID")
            @PathVariable Long id,

            @Valid @RequestBody EventRequest request) {

        return ResponseEntity.ok(
                eventService.updateEvent(id, request)
        );
    }

    // =====================================================
    // DELETE EVENT
    // =====================================================

    @Operation(summary = "Delete an event")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteEvent(

            @Parameter(description = "Event ID")
            @PathVariable Long id) {

        eventService.deleteEvent(id);

        return ResponseEntity.ok("Event deleted successfully.");
    }

    // =====================================================
    // MY EVENTS
    // =====================================================

    @Operation(summary = "Get events created by an organizer")
    @GetMapping("/my-events/{organizerId}")
    public ResponseEntity<List<EventResponse>> getMyEvents(

            @Parameter(description = "Organizer ID")
            @PathVariable Long organizerId) {

        return ResponseEntity.ok(
                eventService.getMyEvents(organizerId)
        );
    }

    // =====================================================
    // UPCOMING EVENTS
    // =====================================================

    @Operation(summary = "Get all upcoming events")
    @GetMapping("/upcoming")
    public ResponseEntity<List<EventResponse>> getUpcomingEvents() {

        return ResponseEntity.ok(
                eventService.getUpcomingEvents()
        );
    }

    // =====================================================
    // EVENTS BY CATEGORY
    // =====================================================

    @Operation(summary = "Get events by category")
    @GetMapping("/category/{category}")
    public ResponseEntity<List<EventResponse>> getEventsByCategory(

            @Parameter(description = "Event category")
            @PathVariable EventCategory category) {

        return ResponseEntity.ok(
                eventService.getEventsByCategory(category)
        );
    }

    // =====================================================
    // CALENDAR EVENTS
    // =====================================================

    @Operation(summary = "Get calendar events within a date range")
    @GetMapping("/calendar")
    public ResponseEntity<List<EventResponse>> getCalendarEvents(

            @Parameter(description = "Zone ID")
            @RequestParam Long zoneId,

            @Parameter(description = "Start DateTime (yyyy-MM-dd'T'HH:mm:ss)")
            @RequestParam String start,

            @Parameter(description = "End DateTime (yyyy-MM-dd'T'HH:mm:ss)")
            @RequestParam String end) {

        Zone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Zone not found with id: " + zoneId));

        LocalDateTime startTime = LocalDateTime.parse(start);

        LocalDateTime endTime = LocalDateTime.parse(end);

        return ResponseEntity.ok(
                eventService.getCalendarEvents(
                        zone,
                        startTime,
                        endTime)
        );
    }

    // =====================================================
    // SEARCH EVENTS
    // =====================================================

    @Operation(summary = "Search events by keyword")
    @GetMapping("/search")
    public ResponseEntity<Page<EventResponse>> searchEvents(

            @Parameter(description = "Search keyword")
            @RequestParam String keyword,

            @Parameter(description = "Page number")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size,

            @Parameter(description = "Sort field")
            @RequestParam(defaultValue = "startTime") String sortBy,

            @Parameter(description = "Sort direction (asc/desc)")
            @RequestParam(defaultValue = "asc") String sortDirection) {

        return ResponseEntity.ok(
                eventService.searchEvents(
                        keyword,
                        page,
                        size,
                        sortBy,
                        sortDirection
                )
        );
    }

}