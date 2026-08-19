package com.neighborhood.eventmanagement.repository;

import com.neighborhood.eventmanagement.entity.Event;
import com.neighborhood.eventmanagement.entity.EventRegistration;
import com.neighborhood.eventmanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventRegistrationRepository extends JpaRepository<EventRegistration, Long> {

    List<EventRegistration> findByUser(User user);

    List<EventRegistration> findByEvent(Event event);

    Optional<EventRegistration> findByEventAndUser(Event event, User user);

    boolean existsByEventAndUser(Event event, User user);

    @Query("""
            SELECT r FROM EventRegistration r
            WHERE r.event = :event AND r.status = 'WAITLISTED'
            ORDER BY r.registeredAt ASC
            """)
    List<EventRegistration> findWaitlistedByEventOrderByDate(@Param("event") Event event);

    @Query("SELECT COUNT(r) FROM EventRegistration r WHERE r.event = :event AND r.status = 'REGISTERED'")
    long countRegisteredByEvent(@Param("event") Event event);
}
