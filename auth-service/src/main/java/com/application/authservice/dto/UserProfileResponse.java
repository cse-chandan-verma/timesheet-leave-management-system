package com.application.authservice.dto;

import lombok.*;
import java.time.LocalDateTime;

/**
 * USER PROFILE RESPONSE DTO
 *
 * What the user sees when they view their profile.
 *
 * Notice: NO password field — never return password
 * even as a BCrypt hash. It serves no purpose to
 * the client and is a security risk.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private Long          id;
    private String        employeeCode;
    private String        fullName;
    private String        email;
    private String        role;
    private boolean       isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}