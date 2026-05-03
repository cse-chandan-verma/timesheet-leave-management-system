package com.application.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AssignManagerRequest {

    @NotBlank(message = "Employee email is required")
    @Email
    private String employeeEmail;

    @NotNull(message = "Manager ID is required")
    private Long managerId;
}
