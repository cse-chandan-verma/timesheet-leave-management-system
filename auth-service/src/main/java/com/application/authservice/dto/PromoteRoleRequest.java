package com.application.authservice.dto;

import jakarta.validation.constraints.*;
import lombok.Data;


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

}