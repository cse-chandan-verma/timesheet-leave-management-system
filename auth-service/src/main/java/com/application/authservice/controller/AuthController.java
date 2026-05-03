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
            @RequestHeader("X-User-Email") String callerEmail,
            @Valid @RequestBody PromoteRoleRequest request) {

        return ResponseEntity.ok(authService.promoteRole(request, callerEmail));
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
    @Operation(summary = "Get all users (Admin/Manager only)")
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        return ResponseEntity.ok(authService.getAllUsers());
    }

    @GetMapping("/admin/user/{email}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get any user's profile (Admin only)")
    public ResponseEntity<UserProfileResponse> getUserProfile(
            @PathVariable String email) {
        return ResponseEntity.ok(authService.getProfile(email));
    }

    @PutMapping("/admin/assign-manager")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign manager to employee (Admin only)", description = "Admin assigns or reassigns a manager to an employee. "
            + "Default manager is the first ADMIN until reassigned.")
    public ResponseEntity<String> assignManager(
            @Valid @RequestBody AssignManagerRequest request) {
        return ResponseEntity.ok(authService.assignManager(request));
    }

    @GetMapping("/manager/my-team")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Get manager's team (Manager only)", description = "Returns all employees assigned to the calling manager.")
    public ResponseEntity<List<UserResponseDto>> getMyTeam(
            @RequestHeader("X-User-Email") String managerEmail) {
        return ResponseEntity.ok(authService.getMyTeam(managerEmail));
    }

    @GetMapping("/admin/managers")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all managers (Admin only)", description = "Returns all users with MANAGER or ADMIN role for assign-manager dropdown.")
    public ResponseEntity<List<UserResponseDto>> getAllManagers() {
        return ResponseEntity.ok(authService.getAllManagers());
    }

    @GetMapping("/internal/manager/{managerId}/employee-ids")
    @Operation(summary = "Internal: get employee IDs for a manager (no auth)", description = "Used by timesheet-service and leave-service internally. Not for frontend.")
    public ResponseEntity<List<Long>> getTeamEmployeeIds(
            @PathVariable Long managerId) {
        return ResponseEntity.ok(authService.getTeamEmployeeIds(managerId));
    }
}