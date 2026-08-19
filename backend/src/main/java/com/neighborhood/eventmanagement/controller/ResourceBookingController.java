package com.neighborhood.eventmanagement.controller;

import com.neighborhood.eventmanagement.entity.*;
import com.neighborhood.eventmanagement.exception.ResourceNotFoundException;
import com.neighborhood.eventmanagement.exception.ValidationException;
import com.neighborhood.eventmanagement.repository.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/resources")
@Tag(name = "Resource Bookings", description = "Resource booking lifecycle (SRS 8.8)")
public class ResourceBookingController {

    private final ResourceRepository resourceRepository;
    private final ResourceBookingRepository bookingRepository;
    private final EventRepository eventRepository;

    public ResourceBookingController(ResourceRepository resourceRepository,
                                     ResourceBookingRepository bookingRepository,
                                     EventRepository eventRepository) {
        this.resourceRepository = resourceRepository;
        this.bookingRepository = bookingRepository;
        this.eventRepository = eventRepository;
    }

    // ── Book a resource ───────────────────────────────────────────────

    @Operation(summary = "Book a resource for an event")
    @PostMapping("/{resourceId}/book")
    public ResponseEntity<ResourceBooking> book(@PathVariable Long resourceId,
                                                @Valid @RequestBody BookingRequest request) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + resourceId));

        Event event = eventRepository.findById(request.eventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + request.eventId()));

        if (!request.endTime().isAfter(request.startTime())) {
            throw new ValidationException("endTime must be after startTime.");
        }

        // Overlap check — reject if any non-cancelled booking overlaps this window
        long overlaps = bookingRepository.countOverlappingBookings(
                resource, request.startTime(), request.endTime(), null);
        if (overlaps > 0) {
            // Quantity check within the overlapping window
            Integer alreadyBooked = bookingRepository.sumBookedQuantityInWindow(
                    resource, request.startTime(), request.endTime());
            if (alreadyBooked == null) alreadyBooked = 0;
            int available = resource.getQuantity() - alreadyBooked;
            if (request.quantity() > available) {
                throw new ValidationException("Only " + available + " unit(s) available in this time window.");
            }
        }

        ResourceBooking booking = new ResourceBooking();
        booking.setResource(resource);
        booking.setEvent(event);
        booking.setQuantityBooked(request.quantity());
        booking.setStartTime(request.startTime());
        booking.setEndTime(request.endTime());
        booking.setStatus(ResourceBooking.BookingStatus.PENDING);

        return ResponseEntity.status(HttpStatus.CREATED).body(bookingRepository.save(booking));
    }

    // ── Get bookings for a resource ───────────────────────────────────

    @Operation(summary = "Get all bookings for a resource")
    @GetMapping("/{resourceId}/bookings")
    public ResponseEntity<List<ResourceBooking>> getBookings(@PathVariable Long resourceId) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + resourceId));
        return ResponseEntity.ok(bookingRepository.findByResource(resource));
    }

    // ── Get available resources for a time window ─────────────────────

    @Operation(summary = "Get resources with available quantity in a time window")
    @GetMapping("/available")
    public ResponseEntity<List<Resource>> getAvailable(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        List<Resource> available = resourceRepository.findAll().stream()
                .filter(r -> {
                    Integer booked = bookingRepository.sumBookedQuantityInWindow(r, startTime, endTime);
                    return (booked == null ? 0 : booked) < r.getQuantity();
                })
                .toList();
        return ResponseEntity.ok(available);
    }

    // ── Cancel a booking ──────────────────────────────────────────────

    @Operation(summary = "Cancel a resource booking")
    @DeleteMapping("/bookings/{bookingId}")
    public ResponseEntity<String> cancelBooking(@PathVariable Long bookingId) {
        ResourceBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
        if (booking.getStatus() == ResourceBooking.BookingStatus.CANCELLED) {
            throw new ValidationException("Booking is already cancelled.");
        }
        booking.setStatus(ResourceBooking.BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        return ResponseEntity.ok("Booking cancelled.");
    }

    // ── Confirm a pending booking ─────────────────────────────────────

    @Operation(summary = "Confirm a pending resource booking")
    @PatchMapping("/bookings/{bookingId}/confirm")
    public ResponseEntity<String> confirmBooking(@PathVariable Long bookingId) {
        ResourceBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
        if (booking.getStatus() != ResourceBooking.BookingStatus.PENDING) {
            throw new ValidationException("Only PENDING bookings can be confirmed.");
        }
        booking.setStatus(ResourceBooking.BookingStatus.CONFIRMED);
        bookingRepository.save(booking);
        return ResponseEntity.ok("Booking confirmed.");
    }

    public record BookingRequest(
            @NotNull Long eventId,
            @NotNull @Min(1) Integer quantity,
            @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {}
}
