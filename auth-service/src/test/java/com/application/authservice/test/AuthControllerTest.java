package com.application.authservice.test;

import com.application.authservice.controller.AuthController;
import com.application.authservice.dto.*;
import com.application.authservice.service.AuthService;
import com.application.authservice.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for unit testing controllers
@DisplayName("AuthController — Web Layer Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /auth/register — success")
    void register_Success() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@test.com");
        request.setPassword("Pass@123");
        request.setConfirmPassword("Pass@123");
        request.setFullName("Test User");
        request.setEmployeeCode("EMP001");

        when(authService.register(any(RegisterRequest.class)))
                .thenReturn("Registration successful");

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().string("Registration successful"));
    }

    @Test
    @DisplayName("POST /auth/login — success")
    void login_Success() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@test.com");
        request.setPassword("pass123");

        AuthResponse response = AuthResponse.builder()
                .token("mock-token")
                .email("test@test.com")
                .build();

        when(authService.login(any(LoginRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock-token"));
    }

    @Test
    @WithMockUser(username = "test@test.com")
    @DisplayName("GET /auth/profile — success")
    void getProfile_Success() throws Exception {
        UserProfileResponse response = UserProfileResponse.builder()
                .email("test@test.com")
                .fullName("Test User")
                .build();

        when(authService.getProfile("test@test.com"))
                .thenReturn(response);

        mockMvc.perform(get("/auth/profile")
                .header("X-User-Email", "test@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@test.com"));
    }

    @Test
    @WithMockUser(username = "test@test.com")
    @DisplayName("PUT /auth/profile — success")
    void updateProfile_Success() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("Updated Name");

        UserProfileResponse response = UserProfileResponse.builder()
                .fullName("Updated Name")
                .build();

        when(authService.updateProfile(anyString(), any(UpdateProfileRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/auth/profile")
                .header("X-User-Email", "test@test.com")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Updated Name"));
    }

    @Test
    @DisplayName("PUT /auth/admin/promote — success")
    void promoteRole_Success() throws Exception {
        PromoteRoleRequest request = new PromoteRoleRequest();
        request.setEmail("user@test.com");
        request.setRole("MANAGER");

        when(authService.promoteRole(any(PromoteRoleRequest.class), anyString()))
                .thenReturn("Role updated successfully");

        mockMvc.perform(put("/auth/admin/promote")
                .header("X-User-Email", "admin@test.com")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Role updated successfully"));
    }

    @Test
    @DisplayName("POST /auth/forgot-password — success")
    void forgotPassword_Success() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("test@test.com");
        request.setNewPassword("NewPass@123");
        request.setConfirmPassword("NewPass@123");

        mockMvc.perform(post("/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Password updated successfully!"));
    }

    @Test
    @DisplayName("GET /auth/users — success")
    void getAllUsers_Success() throws Exception {
        UserResponseDto user = UserResponseDto.builder()
                .email("test@test.com")
                .build();

        when(authService.getAllUsers()).thenReturn(List.of(user));

        mockMvc.perform(get("/auth/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("test@test.com"));
    }
}
