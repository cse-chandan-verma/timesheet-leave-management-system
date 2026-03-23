package com.application.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;      
    private String tokenType;  
    private String role;       
    private Long   employeeId; 
    private String fullName;  
    private String email;      
    private long   expiresIn;  
    
}