package com.application.authservice.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * PROMOTE ROLE REQUEST DTO
 *
 * ADMIN-ONLY endpoint uses this to promote a user's role.
 *
 * Only ADMIN can call /auth/admin/promote.
 * @PreAuthorize("hasRole('ADMIN')") enforces this.
 *
 * Example use cases:
 *   - HR promotes an employee to MANAGER
 *   - Super admin creates another ADMIN
 */
@Data
public class PromoteRoleRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email")
    private String email;
    // Which user to promote

    @NotBlank(message = "Role is required")
    @Pattern(
        regexp = "^(EMPLOYEE|MANAGER|ADMIN)$",
        message = "Role must be EMPLOYEE, MANAGER or ADMIN"
    )
    private String role;
    // The new role to assign
}