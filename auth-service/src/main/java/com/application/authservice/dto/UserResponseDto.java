package com.application.authservice.dto;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponseDto {
    private Long   id;
    private String employeeCode;
    private String fullName;
    private String email;
    private String role;
    private Long   managerId;
    private String managerName;
}
