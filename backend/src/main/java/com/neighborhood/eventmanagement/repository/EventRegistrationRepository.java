package com.neighborhood.eventmanagement.repository;

import com.neighborhood.eventmanagement.entity.EventRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRegistrationRepository
        extends JpaRepository<EventRegistration, Long> {

}