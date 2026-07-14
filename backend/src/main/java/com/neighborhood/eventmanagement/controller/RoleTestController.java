package com.neighborhood.eventmanagement.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RoleTestController {

    @GetMapping("/api/admin/test")
    public String admin() {
        return "ADMIN access granted";
    }

    @GetMapping("/api/events/manage/test")
    public String eventManager() {
        return "COMMUNITY_MANAGER/ADMIN access granted";
    }

    @GetMapping("/api/zones/manage/test")
    public String zoneManager() {
        return "ZONE_COORDINATOR/COMMUNITY_MANAGER/ADMIN access granted";
    }

    @GetMapping("/api/events/organizer/test")
    public String organizer() {
        return "EVENT_ORGANIZER access granted";
    }

    @GetMapping("/api/resident/test")
    public String resident() {
        return "RESIDENT access granted";
    }

    @GetMapping("/api/guest/test")
    public String guest() {
        return "GUEST access granted";
    }
}