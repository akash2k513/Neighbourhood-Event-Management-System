package com.neighborhood.eventmanagement.controller;

import com.neighborhood.eventmanagement.entity.Resource;
import com.neighborhood.eventmanagement.exception.ResourceNotFoundException;
import com.neighborhood.eventmanagement.repository.ResourceRepository;

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
@Tag(name = "Resource Management", description = "APIs for managing bookable resources")
public class ResourceController {

    private final ResourceRepository resourceRepository;

    public ResourceController(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    @Operation(summary = "Get all resources")
    @GetMapping
    public ResponseEntity<List<Resource>> getAllResources() {
        return ResponseEntity.ok(resourceRepository.findAll());
    }

    @Operation(summary = "Get resource by ID")
    @GetMapping("/{id}")
    public ResponseEntity<Resource> getResourceById(@PathVariable Long id) {
        return ResponseEntity.ok(resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + id)));
    }

    @Operation(summary = "Create a resource (Admin)")
    @PostMapping
    public ResponseEntity<Resource> createResource(@Valid @RequestBody ResourceRequest request) {
        Resource resource = new Resource();
        resource.setName(request.name());
        resource.setDescription(request.description());
        resource.setQuantity(request.quantity());
        return ResponseEntity.status(HttpStatus.CREATED).body(resourceRepository.save(resource));
    }

    @Operation(summary = "Update a resource (Admin)")
    @PutMapping("/{id}")
    public ResponseEntity<Resource> updateResource(@PathVariable Long id,
                                                   @Valid @RequestBody ResourceRequest request) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + id));
        resource.setName(request.name());
        resource.setDescription(request.description());
        resource.setQuantity(request.quantity());
        return ResponseEntity.ok(resourceRepository.save(resource));
    }

    @Operation(summary = "Delete a resource (Admin)")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteResource(@PathVariable Long id) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + id));
        resourceRepository.delete(resource);
        return ResponseEntity.ok("Resource deleted successfully.");
    }

    public record ResourceRequest(
            @NotBlank String name,
            String description,
            @NotNull @Min(1) Integer quantity) {}
}
