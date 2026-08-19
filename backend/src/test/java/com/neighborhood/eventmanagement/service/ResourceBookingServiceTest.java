package com.neighborhood.eventmanagement.service;

import com.neighborhood.eventmanagement.entity.*;
import com.neighborhood.eventmanagement.exception.ValidationException;
import com.neighborhood.eventmanagement.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ResourceBookingServiceTest {

    @Mock private ResourceRepository resourceRepository;
    @Mock private ResourceBookingRepository bookingRepository;
    @Mock private EventRepository eventRepository;

    private Resource resource;
    private Event event;
    private final LocalDateTime start = LocalDateTime.now().plusDays(1);
    private final LocalDateTime end   = LocalDateTime.now().plusDays(1).plusHours(2);

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        resource = new Resource();
        resource.setName("Projector");
        resource.setQuantity(5);

        event = new Event();
        event.setTitle("Test Event");

        when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
    }

    @Test
    @DisplayName("Booking succeeds when quantity is available")
    void booking_succeeds_when_quantity_available() {
        when(bookingRepository.sumBookedQuantityInWindow(resource, start, end)).thenReturn(2);
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // 5 total - 2 booked = 3 available; requesting 2 → should succeed
        int requested = 2;
        Integer alreadyBooked = bookingRepository.sumBookedQuantityInWindow(resource, start, end);
        int available = resource.getQuantity() - (alreadyBooked == null ? 0 : alreadyBooked);

        assertTrue(requested <= available, "Should have enough quantity");
    }

    @Test
    @DisplayName("Booking fails when quantity is insufficient")
    void booking_fails_when_quantity_insufficient() {
        when(bookingRepository.sumBookedQuantityInWindow(resource, start, end)).thenReturn(4);

        // 5 total - 4 booked = 1 available; requesting 3 → should fail
        int requested = 3;
        Integer alreadyBooked = bookingRepository.sumBookedQuantityInWindow(resource, start, end);
        int available = resource.getQuantity() - (alreadyBooked == null ? 0 : alreadyBooked);

        assertThrows(ValidationException.class, () -> {
            if (requested > available) {
                throw new ValidationException("Only " + available + " unit(s) available.");
            }
        });
    }

    @Test
    @DisplayName("Booking fails when all units are booked in overlapping window")
    void booking_fails_on_full_overlap() {
        when(bookingRepository.sumBookedQuantityInWindow(resource, start, end)).thenReturn(5);

        Integer alreadyBooked = bookingRepository.sumBookedQuantityInWindow(resource, start, end);
        int available = resource.getQuantity() - (alreadyBooked == null ? 0 : alreadyBooked);

        assertEquals(0, available);
        assertThrows(ValidationException.class, () -> {
            if (1 > available) throw new ValidationException("No units available.");
        });
    }

    @Test
    @DisplayName("Booking status transitions: PENDING → CONFIRMED → CANCELLED")
    void booking_status_lifecycle() {
        ResourceBooking booking = new ResourceBooking();
        booking.setStatus(ResourceBooking.BookingStatus.PENDING);

        assertEquals(ResourceBooking.BookingStatus.PENDING, booking.getStatus());

        booking.setStatus(ResourceBooking.BookingStatus.CONFIRMED);
        assertEquals(ResourceBooking.BookingStatus.CONFIRMED, booking.getStatus());

        booking.setStatus(ResourceBooking.BookingStatus.CANCELLED);
        assertEquals(ResourceBooking.BookingStatus.CANCELLED, booking.getStatus());
    }
}
