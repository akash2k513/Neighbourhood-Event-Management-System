package com.neighborhood.eventmanagement.repository;

import com.neighborhood.eventmanagement.entity.EventApproval;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventApprovalRepository
        extends JpaRepository<EventApproval, Long> {

}