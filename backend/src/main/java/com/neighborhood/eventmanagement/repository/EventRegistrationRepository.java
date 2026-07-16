package com.neighborhood.eventmanagement.repository;

import com.neighborhood.eventmanagement.entity.EventRegistration;
import com.neighborhood.eventmanagement.entity.Event;
import com.neighborhood.eventmanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventRegistrationRepository
        extends JpaRepository<EventRegistration, Long> {

    List<EventRegistration> findByUser(User user);

    List<EventRegistration> findByEvent(Event event);

    Optional<EventRegistration> findByEventAndUser(Event event, User user);

    boolean existsByEventAndUser(Event event, User user);
}