package com.application.authservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.application.authservice.dto.AuthResponse;
import com.application.authservice.dto.LoginRequest;
import com.application.authservice.dto.RegisterRequest;
import com.application.authservice.dto.UserResponse;
import com.application.authservice.service.AuthService;


@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")

    
    public ResponseEntity<String> register(
            @Valid @RequestBody RegisterRequest request) {
        log.info("Register request received for email: {}", request.getEmail());

        String message = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(message);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        log.info("Login request received for email: {}", request.getEmail());
        AuthResponse response = authService.login(request);

        return ResponseEntity.ok(response);
    }
    @GetMapping("/validate")
    public ResponseEntity<String> validateToken(
            @RequestHeader("Authorization") String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Missing or malformed Authorization header");
        }

        String token = authHeader.substring(7);
        return ResponseEntity.ok("Token is valid");
    }
    
    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getUsers(){
    	return ResponseEntity.ok(authService.getAllUsers());
    }
    
    
}