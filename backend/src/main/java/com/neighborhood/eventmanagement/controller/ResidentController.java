package com.neighborhood.eventmanagement.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/resident")
public class ResidentController {

    @GetMapping("/profile")
    public ResponseEntity<?> profile(Authentication authentication) {

        return ResponseEntity.ok(
                "Welcome " + authentication.getName()
        );
    }
}