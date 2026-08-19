package com.neighborhood.eventmanagement.controller;

import com.neighborhood.eventmanagement.entity.Resource;
import com.neighborhood.eventmanagement.entity.Venue;
import com.neighborhood.eventmanagement.exception.ResourceNotFoundException;
import com.neighborhood.eventmanagement.repository.ResourceRepository;
import com.neighborhood.eventmanagement.repository.VenueRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resources")
@Tag(name = "Resources", description = "Resource CRUD (SRS 8.8)")
public class ResourceController {

    private final ResourceRepository resourceRepository;
    private final VenueRepository venueRepository;

    public ResourceController(ResourceRepository resourceRepository, VenueRepository venueRepository) {
        this.resourceRepository = resourceRepository;
        this.venueRepository = venueRepository;
    }

    @Operation(summary = "List all resources")
    @GetMapping
    public ResponseEntity<List<Resource>> getAll(
            @RequestParam(required = false) Resource.ResourceType type,
            @RequestParam(required = false) Long venueId) {

        if (type != null) return ResponseEntity.ok(resourceRepository.findByType(type));
        if (venueId != null) return ResponseEntity.ok(resourceRepository.findByVenueIdOrGlobal(venueId));
        return ResponseEntity.ok(resourceRepository.findAll());
    }

    @Operation(summary = "Get resource by ID")
    @GetMapping("/{id}")
    public ResponseEntity<Resource> getById(@PathVariable Long id) {
        return ResponseEntity.ok(resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + id)));
    }

    @Operation(summary = "Create a resource — Admin/Manager only")
    @PostMapping
    public ResponseEntity<Resource> create(@Valid @RequestBody ResourceRequest request) {
        Resource resource = buildResource(new Resource(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(resourceRepository.save(resource));
    }

    @Operation(summary = "Update a resource — Admin/Manager only")
    @PutMapping("/{id}")
    public ResponseEntity<Resource> update(@PathVariable Long id,
                                           @Valid @RequestBody ResourceRequest request) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + id));
        return ResponseEntity.ok(resourceRepository.save(buildResource(resource, request)));
    }

    @Operation(summary = "Delete a resource — Admin only")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!resourceRepository.existsById(id))
            throw new ResourceNotFoundException("Resource not found: " + id);
        resourceRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ── helpers ──────────────────────────────────────────────────────

    private Resource buildResource(Resource resource, ResourceRequest req) {
        resource.setName(req.name());
        resource.setDescription(req.description());
        resource.setType(req.type());
        resource.setQuantity(req.quantity());
        if (req.venueId() != null) {
            Venue venue = venueRepository.findById(req.venueId())
                    .orElseThrow(() -> new ResourceNotFoundException("Venue not found: " + req.venueId()));
            resource.setVenue(venue);
        } else {
            resource.setVenue(null);
        }
        return resource;
    }

    public record ResourceRequest(
            @NotBlank String name,
            String description,
            @NotNull Resource.ResourceType type,
            @NotNull @Min(1) Integer quantity,
            Long venueId) {}
}
