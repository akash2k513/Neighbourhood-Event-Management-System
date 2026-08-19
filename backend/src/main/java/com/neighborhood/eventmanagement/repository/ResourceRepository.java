package com.neighborhood.eventmanagement.repository;

import com.neighborhood.eventmanagement.entity.Resource;
import com.neighborhood.eventmanagement.entity.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ResourceRepository extends JpaRepository<Resource, Long> {

    List<Resource> findByType(Resource.ResourceType type);

    List<Resource> findByVenue(Venue venue);

    @Query("SELECT r FROM Resource r WHERE r.venue IS NULL OR r.venue.id = :venueId")
    List<Resource> findByVenueIdOrGlobal(@Param("venueId") Long venueId);
}
