package com.neighborhood.eventmanagement.controller;

import com.neighborhood.eventmanagement.entity.Zone;
import com.neighborhood.eventmanagement.exception.ResourceNotFoundException;
import com.neighborhood.eventmanagement.repository.ZoneRepository;

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

    public ZoneController(ZoneRepository zoneRepository) {
        this.zoneRepository = zoneRepository;
    }

    @Operation(summary = "Get all zones")
    @GetMapping
    public ResponseEntity<List<Zone>> getAllZones() {
        return ResponseEntity.ok(zoneRepository.findAll());
    }

    @Operation(summary = "Get zone by ID")
    @GetMapping("/{id}")
    public ResponseEntity<Zone> getZoneById(@PathVariable Long id) {
        return ResponseEntity.ok(zoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found: " + id)));
    }

    @Operation(summary = "Create a new zone (Admin/Zone Coordinator)")
    @PostMapping("/manage")
    public ResponseEntity<Zone> createZone(@Valid @RequestBody ZoneRequest request) {
        Zone zone = new Zone();
        zone.setName(request.name());
        zone.setDescription(request.description());
        return ResponseEntity.status(HttpStatus.CREATED).body(zoneRepository.save(zone));
    }

    @Operation(summary = "Update a zone (Admin/Zone Coordinator)")
    @PutMapping("/manage/{id}")
    public ResponseEntity<Zone> updateZone(@PathVariable Long id,
                                           @Valid @RequestBody ZoneRequest request) {
        Zone zone = zoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found: " + id));
        zone.setName(request.name());
        zone.setDescription(request.description());
        return ResponseEntity.ok(zoneRepository.save(zone));
    }

    @Operation(summary = "Delete a zone (Admin only)")
    @DeleteMapping("/manage/{id}")
    public ResponseEntity<String> deleteZone(@PathVariable Long id) {
        Zone zone = zoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found: " + id));
        zoneRepository.delete(zone);
        return ResponseEntity.ok("Zone deleted successfully.");
    }

    public record ZoneRequest(@NotBlank String name, String description) {}
}
