package com.neighborhood.eventmanagement.repository;

import com.neighborhood.eventmanagement.entity.Venue;
import com.neighborhood.eventmanagement.entity.Zone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface VenueRepository extends JpaRepository<Venue, Long> {

    List<Venue> findByZone(Zone zone);

    List<Venue> findByIsAvailableTrue();

    /**
     * Returns true if the venue already has an event booked
     * that overlaps with the requested [startTime, endTime] window.
     * Used to reject double-bookings (Issue 5).
     */
    @Query("""
            SELECT COUNT(e) FROM Event e
            WHERE e.venue = :venue
              AND e.status NOT IN ('CANCELLED', 'REJECTED')
              AND e.startTime < :endTime
              AND e.endTime   > :startTime
            """)
    long countOverlappingBookings(
            @Param("venue") Venue venue,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}
