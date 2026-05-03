package com.application.authservice.dto;

import lombok.*;
import java.time.LocalDateTime;

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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long          managerId;
    private String        managerName;
    private String        managerEmail;
}
