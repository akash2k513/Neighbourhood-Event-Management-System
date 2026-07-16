package com.neighborhood.eventmanagement.controller;

import com.neighborhood.eventmanagement.dto.ChangePasswordRequest;
import com.neighborhood.eventmanagement.dto.UpdateProfileRequest;
import com.neighborhood.eventmanagement.dto.UserProfileResponse;
import com.neighborhood.eventmanagement.entity.User;
import com.neighborhood.eventmanagement.entity.Zone;
import com.neighborhood.eventmanagement.exception.ResourceNotFoundException;
import com.neighborhood.eventmanagement.exception.ValidationException;
import com.neighborhood.eventmanagement.repository.UserRepository;
import com.neighborhood.eventmanagement.repository.ZoneRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@Tag(name = "User Profile", description = "APIs for viewing and updating the authenticated user's profile")
public class UserController {

    private final UserRepository userRepository;
    private final ZoneRepository zoneRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository,
                          ZoneRepository zoneRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.zoneRepository = zoneRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Operation(summary = "Get current user profile")
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(Authentication authentication) {
        User user = getUser(authentication);
        return ResponseEntity.ok(toResponse(user));
    }

    @Operation(summary = "Update current user profile")
    @PutMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication) {

        User user = getUser(authentication);
        user.setFullName(request.getFullName());

        if (request.getZoneId() != null) {
            Zone zone = zoneRepository.findById(request.getZoneId())
                    .orElseThrow(() -> new ResourceNotFoundException("Zone not found: " + request.getZoneId()));
            user.setZone(zone);
        } else {
            user.setZone(null);
        }

        return ResponseEntity.ok(toResponse(userRepository.save(user)));
    }

    @Operation(summary = "Change password")
    @PutMapping("/change-password")
    public ResponseEntity<String> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {

        User user = getUser(authentication);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new ValidationException("Current password is incorrect.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        return ResponseEntity.ok("Password changed successfully.");
    }

    @Operation(summary = "Delete current user account")
    @DeleteMapping("/account")
    public ResponseEntity<String> deleteAccount(Authentication authentication) {
        User user = getUser(authentication);
        userRepository.delete(user);
        return ResponseEntity.ok("Account deleted successfully.");
    }

    private User getUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

    private UserProfileResponse toResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getZone() != null ? user.getZone().getId() : null,
                user.isEnabled()
        );
    }
}
