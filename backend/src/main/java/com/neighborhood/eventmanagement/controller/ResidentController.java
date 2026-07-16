package com.neighborhood.eventmanagement.controller;

import com.neighborhood.eventmanagement.dto.UserProfileResponse;
import com.neighborhood.eventmanagement.entity.User;
import com.neighborhood.eventmanagement.exception.ResourceNotFoundException;
import com.neighborhood.eventmanagement.repository.UserRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/resident")
@Tag(name = "Resident", description = "APIs accessible to residents and above")
public class ResidentController {

    private final UserRepository userRepository;

    public ResidentController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Operation(summary = "Get current resident profile")
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> profile(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        return ResponseEntity.ok(new UserProfileResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getZone() != null ? user.getZone().getId() : null,
                user.isEnabled()
        ));
    }
}
