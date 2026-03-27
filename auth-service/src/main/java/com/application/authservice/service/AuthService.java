package com.application.authservice.service;

import com.application.authservice.dto.*;
import com.application.authservice.model.User;
import com.application.authservice.repository.UserRepository;
import com.application.authservice.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository        userRepository;
    private final PasswordEncoder       passwordEncoder;
    private final JwtUtil               jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final RabbitTemplate        rabbitTemplate;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    // 1. REGISTER — Always EMPLOYEE, no role choice

    @Transactional
    public String register(RegisterRequest request) {

        // Passwords must match
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match");
        }

        // Email must be unique
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException(
                    "Email is already registered: " + request.getEmail());
        }

        // Employee code must be unique
        if (userRepository.existsByEmployeeCode(
                request.getEmployeeCode())) {
            throw new RuntimeException(
                    "Employee code is already taken: "
                    + request.getEmployeeCode());
        }

        // Build user — role is ALWAYS EMPLOYEE
        // No way for client to override this
        User user = User.builder()
                .employeeCode(request.getEmployeeCode())
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.EMPLOYEE)  // ← hardcoded always
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("New user registered: {} as EMPLOYEE",
                savedUser.getEmail());

        publishUserRegisteredEvent(savedUser);
        return "Registration successful. Welcome, "
                + savedUser.getFullName() + "!";
    }

    // 2. LOGIN

    public AuthResponse login(LoginRequest request) {

        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getEmail(),
                    request.getPassword()
                )
            );
        } catch (BadCredentialsException e) {
            throw new RuntimeException("Invalid email or password");
        } catch (DisabledException e) {
            throw new RuntimeException(
                    "Account is deactivated. Please contact HR.");
        }

        User user = userRepository
                .findByEmailAndIsActiveTrue(request.getEmail())
                .orElseThrow(() -> new RuntimeException(
                        "User not found or account inactive"));

        String token = jwtUtil.generateToken(user);

        log.info("User logged in: {} with role {}",
                user.getEmail(), user.getRole());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .role(user.getRole().name())
                .employeeId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .expiresIn(jwtExpiration)
                .build();
    }

    // 3. GET PROFILE — Any logged-in user

    public UserProfileResponse getProfile(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException(
                        "User not found"));

        return mapToProfileResponse(user);
    }

    // 4. UPDATE PROFILE — Any logged-in user (own profile only)

    @Transactional
    public UserProfileResponse updateProfile(String email,
                                              UpdateProfileRequest request) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException(
                        "User not found"));

        // Update full name if provided
        if (request.getFullName() != null
                && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
            log.info("Updated fullName for user: {}", email);
        }

        // Update password if provided
        if (request.getNewPassword() != null
                && !request.getNewPassword().isBlank()) {

            // Current password is required to change password
            if (request.getCurrentPassword() == null
                    || request.getCurrentPassword().isBlank()) {
                throw new RuntimeException(
                        "Current password is required "
                        + "to set a new password");
            }

            // Verify current password is correct
            if (!passwordEncoder.matches(
                    request.getCurrentPassword(),
                    user.getPassword())) {
                throw new RuntimeException(
                        "Current password is incorrect");
            }

            // New password and confirm must match
            if (!request.getNewPassword().equals(
                    request.getConfirmNewPassword())) {
                throw new RuntimeException(
                        "New password and confirm password "
                        + "do not match");
            }

            // Encode and set new password
            user.setPassword(passwordEncoder.encode(
                    request.getNewPassword()));
            log.info("Password updated for user: {}", email);
        }

        User updated = userRepository.save(user);
        
        // Notify about profile/password update
        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            publishPasswordChangedEvent(updated);
        } else {
            publishProfileUpdatedEvent(updated);
        }

        return mapToProfileResponse(updated);
    }

    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    private UserResponseDto mapToDto(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .name(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
    // 5. PROMOTE ROLE — ADMIN ONLY

    
    @Transactional
    public String promoteRole(PromoteRoleRequest request) {

        User user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException(
                        "User not found with email: "
                        + request.getEmail()));

        // Convert string to enum
        User.Role newRole;
        try {
            newRole = User.Role.valueOf(
                    request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(
                    "Invalid role: " + request.getRole()
                    + ". Must be EMPLOYEE, MANAGER or ADMIN");
        }

        User.Role oldRole = user.getRole();
        user.setRole(newRole);
        userRepository.save(user);

        log.info("Role promoted: {} changed from {} to {} ",
                user.getEmail(), oldRole, newRole);

        // Notify employee
        publishRoleUpdatedEvent(user, oldRole, newRole);

        return "Role updated successfully. "
                + user.getFullName()
                + " is now " + newRole + ". "
                + "They must log out and log in again "
                + "for the new role to take effect.";
    }

    private void publishRoleUpdatedEvent(User user, User.Role oldRole, User.Role newRole) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "USER_ROLE_UPDATED");
            event.put("userId",    user.getId());
            event.put("email",     user.getEmail());
            event.put("fullName",  user.getFullName());
            event.put("oldRole",   oldRole.name());
            event.put("newRole",   newRole.name());
            event.put("timestamp", LocalDateTime.now().toString());

            rabbitTemplate.convertAndSend(
                    "tms.exchange",
                    "user.role.updated",
                    event);
            log.info("Published USER_ROLE_UPDATED event for: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to publish USER_ROLE_UPDATED event: {}", e.getMessage());
        }
    }


    // PRIVATE HELPERS

    private UserProfileResponse mapToProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .employeeCode(user.getEmployeeCode())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .isActive(user.isActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
    
    public void forgotPassword(
            ForgotPasswordRequest request) {
    	
    	User user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException(
                        "No account found with email: "
                        + request.getEmail()));
    	
        // Check passwords match
        if (!request.getNewPassword()
                    .equals(request.getConfirmPassword())) {
            throw new RuntimeException(
                "Passwords do not match");
        }

        user.setPassword(passwordEncoder.encode(
                request.getNewPassword()));
        User savedUser = userRepository.save(user);
        
        // Send notification for password reset
        publishPasswordChangedEvent(savedUser);
    }

    private void publishUserRegisteredEvent(User user) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType",    "USER_REGISTERED");
            event.put("userId",       user.getId());
            event.put("email",        user.getEmail());
            event.put("fullName",     user.getFullName());
            event.put("employeeCode", user.getEmployeeCode());
            event.put("role",         user.getRole().name());
            event.put("timestamp",
                    LocalDateTime.now().toString());

            rabbitTemplate.convertAndSend(
                    "tms.exchange",
                    "user.registered",
                    event);

        } catch (Exception e) {
            log.error("Failed to publish USER_REGISTERED event: {}",
                    e.getMessage());
        }
    }

    private void publishProfileUpdatedEvent(User user) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType",    "USER_PROFILE_UPDATED");
            event.put("userId",       user.getId());
            event.put("email",        user.getEmail());
            event.put("fullName",     user.getFullName());
            event.put("timestamp",    LocalDateTime.now().toString());

            rabbitTemplate.convertAndSend(
                    "tms.exchange",
                    "user.profile.updated",
                    event);
            log.info("Published USER_PROFILE_UPDATED event for: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to publish USER_PROFILE_UPDATED event: {}", e.getMessage());
        }
    }

    private void publishPasswordChangedEvent(User user) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType",    "USER_PASSWORD_CHANGED");
            event.put("userId",       user.getId());
            event.put("email",        user.getEmail());
            event.put("timestamp",    LocalDateTime.now().toString());

            rabbitTemplate.convertAndSend(
                    "tms.exchange",
                    "user.password.changed",
                    event);
            log.info("Published USER_PASSWORD_CHANGED event for: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to publish USER_PASSWORD_CHANGED event: {}", e.getMessage());
        }
    }
}