package com.neighborhood.eventmanagement.repository;

import com.neighborhood.eventmanagement.entity.Zone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ZoneRepository extends JpaRepository<Zone, Long> {

    Optional<Zone> findByName(String name);

}