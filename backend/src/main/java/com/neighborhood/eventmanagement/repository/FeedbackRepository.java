package com.neighborhood.eventmanagement.repository;

import com.neighborhood.eventmanagement.entity.Event;
import com.neighborhood.eventmanagement.entity.Feedback;
import com.neighborhood.eventmanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findByEvent(Event event);
    boolean existsByEventAndUser(Event event, User user);
}
