package com.neighborhood.eventmanagement.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {


    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard(Authentication authentication){

        return ResponseEntity.ok(
                "Welcome Admin "
                + authentication.getName()
        );
    }
}