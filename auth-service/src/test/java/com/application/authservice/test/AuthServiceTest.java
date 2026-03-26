package com.application.authservice.test;

import com.application.authservice.dto.*;
import com.application.authservice.model.User;
import com.application.authservice.repository.UserRepository;
import com.application.authservice.security.JwtUtil;
import com.application.authservice.service.AuthService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService — Unit Tests with Mockito")
class AuthServiceTest {

    // ── Mocks — exact dependencies AuthService declares ──────────────────
    @Mock private UserRepository        userRepository;
    @Mock private PasswordEncoder       passwordEncoder;
    @Mock private JwtUtil               jwtUtil;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private RabbitTemplate        rabbitTemplate;

    @InjectMocks
    private AuthService authService;

    // ── Shared test data ──────────────────────────────────────────────────
    private User       activeUser;
    private User       inactiveUser;

    @BeforeEach
    void setUp() {
        activeUser = User.builder()
                .id(1L)
                .employeeCode("EMP001")
                .fullName("John Doe")
                .email("john@company.com")
                .password("encodedPassword")
                .role(User.Role.EMPLOYEE)
                .isActive(true)
                .build();

        inactiveUser = User.builder()
                .id(2L)
                .employeeCode("EMP002")
                .fullName("Jane Doe")
                .email("jane@company.com")
                .password("encodedPassword")
                .role(User.Role.EMPLOYEE)
                .isActive(false)
                .build();
    }

    // ══════════════════════════════════════════════════════════════════════
    // REGISTER TESTS
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("register() — success: saves user and returns welcome message")
    void register_Success_SavesUserAndReturnsMessage() {
        RegisterRequest request = new RegisterRequest();
        request.setEmployeeCode("EMP003");
        request.setFullName("New User");
        request.setEmail("newuser@company.com");
        request.setPassword("Pass@123");
        request.setConfirmPassword("Pass@123");

        when(userRepository.existsByEmail("newuser@company.com"))
                .thenReturn(false);
        when(userRepository.existsByEmployeeCode("EMP003"))
                .thenReturn(false);
        when(passwordEncoder.encode("Pass@123"))
                .thenReturn("encodedPass");
        when(userRepository.save(any(User.class)))
                .thenReturn(activeUser);

        // RabbitTemplate.convertAndSend — do nothing (void method)
        doNothing().when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(Object.class));

        String result = authService.register(request);

        assertThat(result).contains("Registration successful");
        verify(userRepository, times(1)).save(any(User.class));
        verify(passwordEncoder, times(1)).encode("Pass@123");
    }

    @Test
    @DisplayName("register() — failure: passwords do not match")
    void register_PasswordMismatch_ThrowsRuntimeException() {
        RegisterRequest request = new RegisterRequest();
        request.setPassword("Pass@123");
        request.setConfirmPassword("Different@123");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Passwords do not match");

        // Never reaches DB
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register() — failure: email already registered")
    void register_DuplicateEmail_ThrowsRuntimeException() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("john@company.com");
        request.setPassword("Pass@123");
        request.setConfirmPassword("Pass@123");

        when(userRepository.existsByEmail("john@company.com"))
                .thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register() — failure: employee code already taken")
    void register_DuplicateEmployeeCode_ThrowsRuntimeException() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("unique@company.com");
        request.setEmployeeCode("EMP001");
        request.setPassword("Pass@123");
        request.setConfirmPassword("Pass@123");

        when(userRepository.existsByEmail("unique@company.com"))
                .thenReturn(false);
        when(userRepository.existsByEmployeeCode("EMP001"))
                .thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already taken");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register() — role is always EMPLOYEE regardless of input")
    void register_RoleAlwaysEmployee_NeverAdminOrManager() {
        RegisterRequest request = new RegisterRequest();
        request.setEmployeeCode("EMP010");
        request.setFullName("Hacker");
        request.setEmail("hacker@company.com");
        request.setPassword("Pass@123");
        request.setConfirmPassword("Pass@123");

        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByEmployeeCode(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        doNothing().when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(Object.class));

        // Capture what gets saved
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User saved = inv.getArgument(0);
            // Role must ALWAYS be EMPLOYEE — never ADMIN/MANAGER
            assertThat(saved.getRole()).isEqualTo(User.Role.EMPLOYEE);
            return saved;
        });

        authService.register(request);

        verify(userRepository).save(any(User.class));
    }

    // ══════════════════════════════════════════════════════════════════════
    // LOGIN TESTS
    // ══════════════════════════════════════════════════════════════════════

//    @Test
//    @DisplayName("login() — success: valid credentials return AuthResponse with token")
//    void login_ValidCredentials_ReturnsAuthResponseWithToken() {
//        LoginRequest request = new LoginRequest();
//        request.setEmail("john@company.com");
//        request.setPassword("pass@123");
//
//        // AuthenticationManager — do nothing (success means no exception)
//        doNothing().when(authenticationManager)
//                .authenticate(any(UsernamePasswordAuthenticationToken.class));
//
//        when(userRepository.findByEmailAndIsActiveTrue("john@company.com"))
//                .thenReturn(Optional.of(activeUser));
//        when(jwtUtil.generateToken(activeUser))
//                .thenReturn("mock.jwt.token");
//
//        AuthResponse response = authService.login(request);
//
//        assertThat(response).isNotNull();
//        assertThat(response.getToken()).isEqualTo("mock.jwt.token");
//        assertThat(response.getEmail()).isEqualTo("john@company.com");
//        assertThat(response.getRole()).isEqualTo("EMPLOYEE");
//        assertThat(response.getTokenType()).isEqualTo("Bearer");
//
//        // Verify jwtUtil.generateToken was called with correct user
//        verify(jwtUtil, times(1)).generateToken(activeUser);
//    }

    @Test
    @DisplayName("login() — failure: wrong password throws RuntimeException")
    void login_WrongPassword_ThrowsRuntimeException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("john@company.com");
        request.setPassword("WrongPass");

        // AuthenticationManager throws BadCredentialsException
        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid email or password");

        // Never reaches jwtUtil
        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    @DisplayName("login() — failure: inactive account throws RuntimeException")
    void login_InactiveAccount_ThrowsRuntimeException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("jane@company.com");
        request.setPassword("Pass@123");

        // AuthenticationManager throws DisabledException for inactive users
        doThrow(new DisabledException("Account disabled"))
                .when(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("deactivated");

        verify(jwtUtil, never()).generateToken(any());
    }

//    @Test
//    @DisplayName("login() — failure: user not found after auth passes")
//    void login_UserNotFoundAfterAuth_ThrowsRuntimeException() {
//        LoginRequest request = new LoginRequest();
//        request.setEmail("ghost@company.com");
//        request.setPassword("Pass@123");
//
//        doNothing().when(authenticationManager)
//                .authenticate(any(UsernamePasswordAuthenticationToken.class));
//
//        when(userRepository.findByEmailAndIsActiveTrue("ghost@company.com"))
//                .thenReturn(Optional.empty());
//
//        assertThatThrownBy(() -> authService.login(request))
//                .isInstanceOf(RuntimeException.class)
//                .hasMessageContaining("not found");
//
//        verify(jwtUtil, never()).generateToken(any());
//    }

    // ══════════════════════════════════════════════════════════════════════
    // GET PROFILE TESTS
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("getProfile() — success: returns UserProfileResponse")
    void getProfile_ExistingEmail_ReturnsProfile() {
        when(userRepository.findByEmail("john@company.com"))
                .thenReturn(Optional.of(activeUser));

        UserProfileResponse profile =
                authService.getProfile("john@company.com");

        assertThat(profile).isNotNull();
        assertThat(profile.getEmail()).isEqualTo("john@company.com");
        assertThat(profile.getFullName()).isEqualTo("John Doe");
        assertThat(profile.getRole()).isEqualTo("EMPLOYEE");
        assertThat(profile.isActive()).isTrue();
    }

    @Test
    @DisplayName("getProfile() — failure: unknown email throws RuntimeException")
    void getProfile_UnknownEmail_ThrowsRuntimeException() {
        when(userRepository.findByEmail("nobody@company.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                authService.getProfile("nobody@company.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");
    }

    // ══════════════════════════════════════════════════════════════════════
    // UPDATE PROFILE TESTS
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("updateProfile() — success: fullName updated")
    void updateProfile_FullNameUpdate_SavesCorrectly() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("John Updated");

        when(userRepository.findByEmail("john@company.com"))
                .thenReturn(Optional.of(activeUser));
        when(userRepository.save(any(User.class)))
                .thenReturn(activeUser);

        UserProfileResponse result =
                authService.updateProfile("john@company.com", request);

        assertThat(result).isNotNull();
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("updateProfile() — success: password changed with correct current password")
    void updateProfile_PasswordChange_Success() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setCurrentPassword("Pass@123");
        request.setNewPassword("NewPass@456");
        request.setConfirmNewPassword("NewPass@456");

        when(userRepository.findByEmail("john@company.com"))
                .thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("Pass@123", "encodedPassword"))
                .thenReturn(true);
        when(passwordEncoder.encode("NewPass@456"))
                .thenReturn("newEncodedPass");
        when(userRepository.save(any(User.class)))
                .thenReturn(activeUser);

        assertThatNoException().isThrownBy(() ->
                authService.updateProfile("john@company.com", request));

        verify(passwordEncoder).encode("NewPass@456");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("updateProfile() — failure: wrong current password")
    void updateProfile_WrongCurrentPassword_ThrowsRuntimeException() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setCurrentPassword("WrongPass");
        request.setNewPassword("NewPass@456");
        request.setConfirmNewPassword("NewPass@456");

        when(userRepository.findByEmail("john@company.com"))
                .thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("WrongPass", "encodedPassword"))
                .thenReturn(false);

        assertThatThrownBy(() ->
                authService.updateProfile("john@company.com", request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Current password is incorrect");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateProfile() — failure: new passwords do not match")
    void updateProfile_NewPasswordMismatch_ThrowsRuntimeException() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setCurrentPassword("Pass@123");
        request.setNewPassword("NewPass@456");
        request.setConfirmNewPassword("Different@789");

        when(userRepository.findByEmail("john@company.com"))
                .thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("Pass@123", "encodedPassword"))
                .thenReturn(true);

        assertThatThrownBy(() ->
                authService.updateProfile("john@company.com", request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("do not match");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateProfile() — failure: new password provided without current password")
    void updateProfile_NewPasswordWithoutCurrentPassword_ThrowsRuntimeException() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setNewPassword("NewPass@456");
        request.setConfirmNewPassword("NewPass@456");
        // currentPassword not set

        when(userRepository.findByEmail("john@company.com"))
                .thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() ->
                authService.updateProfile("john@company.com", request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Current password is required");
    }

    // ══════════════════════════════════════════════════════════════════════
    // PROMOTE ROLE TESTS
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("promoteRole() — success: EMPLOYEE promoted to MANAGER")
    void promoteRole_EmployeeToManager_Success() {
        PromoteRoleRequest request = new PromoteRoleRequest();
        request.setEmail("john@company.com");
        request.setRole("MANAGER");

        when(userRepository.findByEmail("john@company.com"))
                .thenReturn(Optional.of(activeUser));
        when(userRepository.save(any(User.class)))
                .thenReturn(activeUser);

        String result = authService.promoteRole(request);

        assertThat(result).contains("MANAGER");
        verify(userRepository).save(argThat(u ->
                u.getRole() == User.Role.MANAGER));
    }

    @Test
    @DisplayName("promoteRole() — success: EMPLOYEE promoted to ADMIN")
    void promoteRole_EmployeeToAdmin_Success() {
        PromoteRoleRequest request = new PromoteRoleRequest();
        request.setEmail("john@company.com");
        request.setRole("ADMIN");

        when(userRepository.findByEmail("john@company.com"))
                .thenReturn(Optional.of(activeUser));
        when(userRepository.save(any(User.class)))
                .thenReturn(activeUser);

        String result = authService.promoteRole(request);

        assertThat(result).contains("ADMIN");
        verify(userRepository).save(argThat(u ->
                u.getRole() == User.Role.ADMIN));
    }

    @Test
    @DisplayName("promoteRole() — failure: invalid role string throws RuntimeException")
    void promoteRole_InvalidRole_ThrowsRuntimeException() {
        PromoteRoleRequest request = new PromoteRoleRequest();
        request.setEmail("john@company.com");
        request.setRole("SUPERUSER");

        when(userRepository.findByEmail("john@company.com"))
                .thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() -> authService.promoteRole(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid role");
    }

    @Test
    @DisplayName("promoteRole() — failure: user not found throws RuntimeException")
    void promoteRole_UserNotFound_ThrowsRuntimeException() {
        PromoteRoleRequest request = new PromoteRoleRequest();
        request.setEmail("nobody@company.com");
        request.setRole("MANAGER");

        when(userRepository.findByEmail("nobody@company.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.promoteRole(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    // ══════════════════════════════════════════════════════════════════════
    // FORGOT PASSWORD TESTS
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("forgotPassword() — success: password reset correctly")
    void forgotPassword_ValidRequest_ResetsPassword() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("john@company.com");
        request.setNewPassword("Reset@123");
        request.setConfirmPassword("Reset@123");

        when(userRepository.findByEmail("john@company.com"))
                .thenReturn(Optional.of(activeUser));
        when(passwordEncoder.encode("Reset@123"))
                .thenReturn("encodedReset");
        when(userRepository.save(any(User.class)))
                .thenReturn(activeUser);

        assertThatNoException().isThrownBy(() ->
                authService.forgotPassword(request));

        verify(passwordEncoder).encode("Reset@123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("forgotPassword() — failure: email not found")
    void forgotPassword_EmailNotFound_ThrowsRuntimeException() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("nobody@company.com");
        request.setNewPassword("Reset@123");
        request.setConfirmPassword("Reset@123");

        when(userRepository.findByEmail("nobody@company.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.forgotPassword(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No account found");
    }

    @Test
    @DisplayName("forgotPassword() — failure: passwords do not match")
    void forgotPassword_PasswordMismatch_ThrowsRuntimeException() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("john@company.com");
        request.setNewPassword("Reset@123");
        request.setConfirmPassword("Different@456");

        when(userRepository.findByEmail("john@company.com"))
                .thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() -> authService.forgotPassword(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Passwords do not match");

        verify(userRepository, never()).save(any());
    }
}
