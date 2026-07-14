package com.neighborhood.eventmanagement.repository;

import com.neighborhood.eventmanagement.entity.Venue;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VenueRepository extends JpaRepository<Venue, Long> {

}