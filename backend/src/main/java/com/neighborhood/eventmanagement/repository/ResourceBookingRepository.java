package com.neighborhood.eventmanagement.repository;

import com.neighborhood.eventmanagement.entity.Event;
import com.neighborhood.eventmanagement.entity.Resource;
import com.neighborhood.eventmanagement.entity.ResourceBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ResourceBookingRepository extends JpaRepository<ResourceBooking, Long> {

    List<ResourceBooking> findByEvent(Event event);

    List<ResourceBooking> findByResource(Resource resource);

    @Query("""
            SELECT COALESCE(SUM(rb.quantityBooked), 0)
            FROM ResourceBooking rb
            WHERE rb.resource = :resource
              AND rb.status <> 'CANCELLED'
              AND rb.startTime < :endTime
              AND rb.endTime > :startTime
            """)
    Integer sumBookedQuantityInWindow(
            @Param("resource") Resource resource,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Query("""
            SELECT COUNT(rb)
            FROM ResourceBooking rb
            WHERE rb.resource = :resource
              AND rb.status <> 'CANCELLED'
              AND rb.startTime < :endTime
              AND rb.endTime > :startTime
              AND (:excludeId IS NULL OR rb.id <> :excludeId)
            """)
    long countOverlappingBookings(
            @Param("resource") Resource resource,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludeId") Long excludeId);
}
