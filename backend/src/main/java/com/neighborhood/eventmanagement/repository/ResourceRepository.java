package com.neighborhood.eventmanagement.repository;

import com.neighborhood.eventmanagement.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceRepository extends JpaRepository<Resource, Long> {

}