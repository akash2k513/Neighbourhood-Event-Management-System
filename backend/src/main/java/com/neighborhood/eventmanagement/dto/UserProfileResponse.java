package com.neighborhood.eventmanagement.dto;

import com.neighborhood.eventmanagement.entity.Role;

public class UserProfileResponse {

    private Long id;
    private String fullName;
    private String email;
    private Role role;
    private Long zoneId;
    private boolean enabled;

    public UserProfileResponse() {
    }

    public UserProfileResponse(Long id,
                               String fullName,
                               String email,
                               Role role,
                               Long zoneId,
                               boolean enabled) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.zoneId = zoneId;
        this.enabled = enabled;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public void setZoneId(Long zoneId) {
        this.zoneId = zoneId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}