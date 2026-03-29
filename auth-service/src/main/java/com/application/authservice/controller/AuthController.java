package com.application.authservice.controller;

import com.application.authservice.dto.*;
import com.application.authservice.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Auth, Profile and Role Management APIs")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register new employee", description = "Creates a new account. Role is always EMPLOYEE. "
            + "Role promotion done separately by Admin only.")
    public ResponseEntity<String> register(
            @Valid @RequestBody RegisterRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Returns JWT token with role and user info")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get my profile", description = "Returns profile of the currently logged-in user")
    public ResponseEntity<UserProfileResponse> getProfile(
            @RequestHeader("X-User-Email") String email) {
        // X-User-Email is added by the Gateway after JWT validation
        // On Swagger, add it manually in the header

        return ResponseEntity.ok(authService.getProfile(email));
    }

    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update my profile", description = "Update fullName or password. "
            + "To change password, provide currentPassword too.")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @RequestHeader("X-User-Email") String email,
            @Valid @RequestBody UpdateProfileRequest request) {

        return ResponseEntity.ok(
                authService.updateProfile(email, request));
    }

    @PutMapping("/admin/promote")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Promote user role (Admin only)", description = "Only ADMIN can promote a user to MANAGER or ADMIN. "
            + "User must re-login after promotion for new role to apply.")
    public ResponseEntity<String> promoteRole(
            @Valid @RequestBody PromoteRoleRequest request) {

        return ResponseEntity.ok(authService.promoteRole(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(
                "Password updated successfully!");
    }

    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Get all users (Admin/Manager only)", description = "Only Admin and Manager have the access to see all the users")
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        return ResponseEntity.ok(authService.getAllUsers());
    }

    @GetMapping("/admin/user/{email}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get any user's profile (Admin only)", description = "Admin can look up any employee's profile by email")
    public ResponseEntity<UserProfileResponse> getUserProfile(
            @PathVariable String email) {

        return ResponseEntity.ok(authService.getProfile(email));
    }
}