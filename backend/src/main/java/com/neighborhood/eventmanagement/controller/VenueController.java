package com.neighborhood.eventmanagement.controller;

import com.neighborhood.eventmanagement.entity.Role;
import com.neighborhood.eventmanagement.entity.User;
import com.neighborhood.eventmanagement.entity.Venue;
import com.neighborhood.eventmanagement.entity.Zone;
import com.neighborhood.eventmanagement.exception.ResourceNotFoundException;
import com.neighborhood.eventmanagement.exception.UnauthorizedAccessException;
import com.neighborhood.eventmanagement.exception.ValidationException;
import com.neighborhood.eventmanagement.repository.EventRepository;
import com.neighborhood.eventmanagement.repository.UserRepository;
import com.neighborhood.eventmanagement.repository.VenueRepository;
import com.neighborhood.eventmanagement.repository.ZoneRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/venues")
@Tag(name = "Venue Management", description = "APIs for managing venues (SRS 8.7)")
public class VenueController {

    private final VenueRepository venueRepository;
    private final ZoneRepository zoneRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    public VenueController(VenueRepository venueRepository,
                           ZoneRepository zoneRepository,
                           UserRepository userRepository,
                           EventRepository eventRepository) {
        this.venueRepository = venueRepository;
        this.zoneRepository = zoneRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
    }

    // ── Public / authenticated read ──────────────────────────────────

    @Operation(summary = "Get all venues")
    @GetMapping
    public ResponseEntity<List<Venue>> getAllVenues() {
        return ResponseEntity.ok(venueRepository.findAll());
    }

    @Operation(summary = "Get venue by ID")
    @GetMapping("/{id}")
    public ResponseEntity<Venue> getVenueById(@PathVariable Long id) {
        return ResponseEntity.ok(findVenue(id));
    }

    @Operation(summary = "Get all available venues")
    @GetMapping("/available")
    public ResponseEntity<List<Venue>> getAvailableVenues() {
        return ResponseEntity.ok(venueRepository.findByIsAvailableTrue());
    }

    @Operation(summary = "Get venues by zone")
    @GetMapping("/zone/{zoneId}")
    public ResponseEntity<List<Venue>> getVenuesByZone(@PathVariable Long zoneId) {
        Zone zone = findZone(zoneId);
        return ResponseEntity.ok(venueRepository.findByZone(zone));
    }

    @Operation(summary = "Get events booked at a venue")
    @GetMapping("/{id}/bookings")
    public ResponseEntity<?> getVenueBookings(@PathVariable Long id) {
        Venue venue = findVenue(id);
        return ResponseEntity.ok(eventRepository.findByVenue(venue));
    }

    // ── Create (Admin / Community Manager) ──────────────────────────

    @Operation(summary = "Create a venue (Admin / Community Manager)")
    @PostMapping
    public ResponseEntity<Venue> createVenue(@Valid @RequestBody VenueRequest request,
                                             Authentication authentication) {
        Zone zone = request.zoneId() != null ? findZone(request.zoneId()) : null;

        // ZONE_COORDINATOR can only create venues in their own zone
        enforceZoneAccess(authentication, zone);

        Venue venue = buildVenue(new Venue(), request, zone);
        return ResponseEntity.status(HttpStatus.CREATED).body(venueRepository.save(venue));
    }

    // ── Update (Admin / Community Manager / Zone Coordinator own zone) ─

    @Operation(summary = "Update a venue")
    @PutMapping("/{id}")
    public ResponseEntity<Venue> updateVenue(@PathVariable Long id,
                                             @Valid @RequestBody VenueRequest request,
                                             Authentication authentication) {
        Venue venue = findVenue(id);
        Zone zone = request.zoneId() != null ? findZone(request.zoneId()) : null;

        enforceZoneAccess(authentication, venue.getZone());

        buildVenue(venue, request, zone);
        return ResponseEntity.ok(venueRepository.save(venue));
    }

    // ── Delete (Admin only) ──────────────────────────────────────────

    @Operation(summary = "Delete a venue (Admin only)")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteVenue(@PathVariable Long id) {
        venueRepository.delete(findVenue(id));
        return ResponseEntity.ok("Venue deleted successfully.");
    }

    // ── Double-booking check ─────────────────────────────────────────

    @Operation(summary = "Check if a venue is available for a time window")
    @GetMapping("/{id}/check-availability")
    public ResponseEntity<Boolean> checkAvailability(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        Venue venue = findVenue(id);
        boolean hasOverlap = venueRepository.countOverlappingBookings(venue, startTime, endTime) > 0;
        return ResponseEntity.ok(!hasOverlap);
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private Venue buildVenue(Venue venue, VenueRequest req, Zone zone) {
        venue.setName(req.name());
        venue.setAddress(req.address());
        venue.setCapacity(req.capacity());
        venue.setHourlyRate(req.hourlyRate());
        venue.setAccessibilityFeatures(req.accessibilityFeatures());
        venue.setIsAvailable(req.isAvailable() != null ? req.isAvailable() : true);
        venue.setZone(zone);
        return venue;
    }

    /**
     * ZONE_COORDINATOR may only manage venues in their own assigned zone (FR3, FR7).
     * COMMUNITY_MANAGER and ADMIN have no zone restriction.
     */
    private void enforceZoneAccess(Authentication authentication, Zone targetZone) {
        if (authentication == null) return;

        User actor = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        if (actor.getRole() == Role.ZONE_COORDINATOR) {
            if (targetZone == null) {
                throw new ValidationException("Zone Coordinators must assign a zone to the venue.");
            }
            if (actor.getZone() == null || !actor.getZone().getId().equals(targetZone.getId())) {
                throw new UnauthorizedAccessException(
                        "Zone Coordinators can only manage venues in their own zone.");
            }
        }
    }

    private Venue findVenue(Long id) {
        return venueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found: " + id));
    }

    private Zone findZone(Long id) {
        return zoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found: " + id));
    }

    public record VenueRequest(
            @NotBlank String name,
            String address,
            @NotNull @Min(1) Integer capacity,
            Double hourlyRate,
            String accessibilityFeatures,
            Boolean isAvailable,
            Long zoneId) {}
}
