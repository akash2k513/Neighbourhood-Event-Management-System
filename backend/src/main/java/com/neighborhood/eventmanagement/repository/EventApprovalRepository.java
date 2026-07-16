package com.neighborhood.eventmanagement.repository;

import com.neighborhood.eventmanagement.entity.EventApproval;
import com.neighborhood.eventmanagement.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EventApprovalRepository
        extends JpaRepository<EventApproval, Long> {

    Optional<EventApproval> findByEvent(Event event);
}