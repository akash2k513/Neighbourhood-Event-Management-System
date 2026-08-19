package com.neighborhood.eventmanagement.controller;

import com.neighborhood.eventmanagement.dto.EventResponse;
import com.neighborhood.eventmanagement.dto.UserProfileResponse;
import com.neighborhood.eventmanagement.entity.Zone;
import com.neighborhood.eventmanagement.exception.ResourceNotFoundException;
import com.neighborhood.eventmanagement.repository.EventRepository;
import com.neighborhood.eventmanagement.repository.UserRepository;
import com.neighborhood.eventmanagement.repository.ZoneRepository;
import com.neighborhood.eventmanagement.util.EventMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/zones")
@Tag(name = "Zone Management", description = "APIs for managing neighborhood zones")
public class ZoneController {

    private final ZoneRepository zoneRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public ZoneController(ZoneRepository zoneRepository,
                          EventRepository eventRepository,
                          UserRepository userRepository) {
        this.zoneRepository = zoneRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    // ── Public read ──────────────────────────────────────────────────

    @Operation(summary = "Get all zones")
    @GetMapping
    public ResponseEntity<List<Zone>> getAllZones() {
        return ResponseEntity.ok(zoneRepository.findAll());
    }

    @Operation(summary = "Get zone by ID")
    @GetMapping("/{id}")
    public ResponseEntity<Zone> getZoneById(@PathVariable Long id) {
        return ResponseEntity.ok(findZone(id));
    }

    @Operation(summary = "Get all events in a zone")
    @GetMapping("/{id}/events")
    public ResponseEntity<List<EventResponse>> getZoneEvents(@PathVariable Long id) {
        Zone zone = findZone(id);
        List<EventResponse> events = eventRepository.findByZone(zone)
                .stream().map(EventMapper::toResponse).toList();
        return ResponseEntity.ok(events);
    }

    @Operation(summary = "Get all residents in a zone")
    @GetMapping("/{id}/residents")
    public ResponseEntity<List<UserProfileResponse>> getZoneResidents(@PathVariable Long id) {
        Zone zone = findZone(id);
        List<UserProfileResponse> residents = userRepository.findByZone(zone)
                .stream()
                .map(u -> new UserProfileResponse(
                        u.getId(), u.getFullName(), u.getEmail(), u.getRole(),
                        u.getZone() != null ? u.getZone().getId() : null,
                        u.isEnabled()))
                .toList();
        return ResponseEntity.ok(residents);
    }

    // ── Manage (ZONE_COORDINATOR / COMMUNITY_MANAGER / ADMIN) ────────

    @Operation(summary = "Create a new zone (Admin only)")
    @PostMapping("/manage")
    public ResponseEntity<Zone> createZone(@Valid @RequestBody ZoneRequest request) {
        Zone zone = new Zone();
        zone.setName(request.name());
        zone.setDescription(request.description());
        return ResponseEntity.status(HttpStatus.CREATED).body(zoneRepository.save(zone));
    }

    @Operation(summary = "Update a zone (Admin / Zone Coordinator)")
    @PutMapping("/manage/{id}")
    public ResponseEntity<Zone> updateZone(@PathVariable Long id,
                                           @Valid @RequestBody ZoneRequest request) {
        Zone zone = findZone(id);
        zone.setName(request.name());
        zone.setDescription(request.description());
        return ResponseEntity.ok(zoneRepository.save(zone));
    }

    @Operation(summary = "Delete a zone (Admin only)")
    @DeleteMapping("/manage/{id}")
    public ResponseEntity<String> deleteZone(@PathVariable Long id) {
        zoneRepository.delete(findZone(id));
        return ResponseEntity.ok("Zone deleted successfully.");
    }

    // ── Helper ───────────────────────────────────────────────────────

    private Zone findZone(Long id) {
        return zoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found: " + id));
    }

    public record ZoneRequest(@NotBlank String name, String description) {}
}
