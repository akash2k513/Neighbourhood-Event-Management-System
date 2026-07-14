package com.neighborhood.eventmanagement.specification;

import com.neighborhood.eventmanagement.entity.Event;
import com.neighborhood.eventmanagement.entity.EventCategory;
import com.neighborhood.eventmanagement.entity.EventStatus;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

public class EventSpecification {

    private EventSpecification() {
    }

    // =====================================================
    // CATEGORY FILTER
    // =====================================================

    public static Specification<Event> hasCategory(EventCategory category) {

        return (root, query, cb) ->

                category == null
                        ? cb.conjunction()
                        : cb.equal(root.get("category"), category);
    }

    // =====================================================
    // STATUS FILTER
    // =====================================================

    public static Specification<Event> hasStatus(EventStatus status) {

        return (root, query, cb) ->

                status == null
                        ? cb.conjunction()
                        : cb.equal(root.get("status"), status);
    }

    // =====================================================
    // ZONE FILTER
    // =====================================================

    public static Specification<Event> hasZoneId(Long zoneId) {

        return (root, query, cb) ->

                zoneId == null
                        ? cb.conjunction()
                        : cb.equal(root.join("zone", JoinType.LEFT).get("id"), zoneId);
    }

    // =====================================================
    // KEYWORD SEARCH
    // =====================================================

    public static Specification<Event> keywordContains(String keyword) {

        return (root, query, cb) -> {

            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction();
            }

            String search = "%" + keyword.toLowerCase() + "%";

            return cb.or(

                    cb.like(cb.lower(root.get("title")), search),

                    cb.like(cb.lower(root.get("description")), search),

                    cb.like(cb.lower(root.get("location")), search)

            );
        };
    }

}