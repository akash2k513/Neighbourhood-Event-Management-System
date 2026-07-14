package com.neighborhood.eventmanagement.repository;

import com.neighborhood.eventmanagement.entity.ResourceBooking;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceBookingRepository
        extends JpaRepository<ResourceBooking, Long> {

}