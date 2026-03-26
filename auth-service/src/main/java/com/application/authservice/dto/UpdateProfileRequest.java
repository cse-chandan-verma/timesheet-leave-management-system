package com.application.authservice.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * UPDATE PROFILE REQUEST DTO
 *
 * What an employee can update about themselves:
 *   - Full name
 *   - Password (with current password verification)
 *
 * What they CANNOT update:
 *   - Email (used as login identifier — changing it
 *     would break authentication)
 *   - Employee code (assigned by HR — immutable)
 *   - Role (only ADMIN can change this)
 */
@Data
public class UpdateProfileRequest {

    @Size(min = 2, max = 100,
          message = "Full name must be between 2 and 100 characters")
    private String fullName;
    // Optional — only updated if provided (not null)

    private String currentPassword;
    // Required ONLY when changing password
    // Must match the stored BCrypt hash

    @Size(min = 8,
          message = "New password must be at least 8 characters")
    @Pattern(
        regexp = "^^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[#?!@$%^&*-]).{8,}$",
        message = "New password must have uppercase, lowercase and number"
    )
    private String newPassword;
    // Optional — only updated if provided

    private String confirmNewPassword;
    // Must match newPassword when changing password
}