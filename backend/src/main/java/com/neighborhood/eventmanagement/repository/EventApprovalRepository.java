package com.neighborhood.eventmanagement.repository;

import com.neighborhood.eventmanagement.entity.EventApproval;
import com.neighborhood.eventmanagement.entity.Event;
import com.neighborhood.eventmanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventApprovalRepository extends JpaRepository<EventApproval, Long> {

    Optional<EventApproval> findByEvent(Event event);

    List<EventApproval> findByStatus(EventApproval.ApprovalStatus status);

    List<EventApproval> findByApprovedBy(User approvedBy);

    @Query("SELECT a FROM EventApproval a WHERE a.status <> 'PENDING' ORDER BY a.approvedAt DESC")
    List<EventApproval> findHistory();

    @Query("SELECT a FROM EventApproval a WHERE a.event.zone.id = :zoneId AND a.status = 'PENDING'")
    List<EventApproval> findPendingByZone(@Param("zoneId") Long zoneId);
}
