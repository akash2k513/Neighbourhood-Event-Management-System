package com.neighborhood.eventmanagement.repository;

import com.neighborhood.eventmanagement.entity.Event;
import com.neighborhood.eventmanagement.entity.EventCategory;
import com.neighborhood.eventmanagement.entity.EventStatus;
import com.neighborhood.eventmanagement.entity.User;
import com.neighborhood.eventmanagement.entity.Zone;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long>,
        JpaSpecificationExecutor<Event> {

    // SRS 11.1.1 — findUpcomingEventsByStatus
    @Query("""
            SELECT e FROM Event e
            WHERE e.status = :status
              AND e.startTime >= :currentTime
            ORDER BY e.startTime ASC
            """)
    List<Event> findUpcomingEventsByStatus(
            @Param("status") EventStatus status,
            @Param("currentTime") LocalDateTime currentTime);

    // SRS 11.1.1 — findEventsByZoneAndDateRange
    @Query("""
            SELECT e FROM Event e
            WHERE e.zone = :zone
              AND e.startTime BETWEEN :startDate AND :endDate
            ORDER BY e.startTime ASC
            """)
    List<Event> findEventsByZoneAndDateRange(
            @Param("zone") Zone zone,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // SRS 11.1.1 — searchEventsByKeyword (paginated)
    @Query("""
            SELECT e FROM Event e
            WHERE LOWER(e.title)       LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(e.location)    LIKE LOWER(CONCAT('%', :keyword, '%'))
            """)
    Page<Event> searchEventsByKeyword(
            @Param("keyword") String keyword,
            Pageable pageable);

    // SRS 11.1.1 — findEventsByOrganizer
    @Query("""
            SELECT e FROM Event e
            WHERE e.organizer = :organizer
            ORDER BY e.createdAt DESC
            """)
    List<Event> findByOrganizer(@Param("organizer") User organizer);

    // Derived query — findByCategory
    List<Event> findByCategory(EventCategory category);
}
