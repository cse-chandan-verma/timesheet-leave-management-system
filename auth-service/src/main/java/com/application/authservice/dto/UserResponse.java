package com.application.authservice.dto;

import java.time.LocalDateTime;

import com.application.authservice.model.User.Role;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserResponse {
	private Long id;
	private String employeeCode;
	private String fullName;
	private String email;
	private Role role;
	private boolean isActive;
	private LocalDateTime createdAt;
}
