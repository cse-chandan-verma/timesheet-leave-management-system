package com.application.authservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.authservice.dto.AuthResponse;
import com.application.authservice.dto.LoginRequest;
import com.application.authservice.dto.RegisterRequest;
import com.application.authservice.exception.AuthException;
import com.application.authservice.model.User;
import com.application.authservice.repository.UserRepository;
import com.application.authservice.security.JwtUtil;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final RabbitTemplate rabbitTemplate;  

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Transactional
    public String register(RegisterRequest request) {

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new AuthException("Passwords do not match");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AuthException("Email is already registered: "
                    + request.getEmail());
        }

        if (userRepository.existsByEmployeeCode(request.getEmployeeCode())) {
            throw new AuthException("Employee code is already taken: "
                    + request.getEmployeeCode());
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User user = User.builder()   
                .employeeCode(request.getEmployeeCode())
                .fullName(request.getFullName())
                .email(request.getEmail())
               .password(encodedPassword)
                .role(User.Role.EMPLOYEE)
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);

        log.info("New user registered: {} ({})",   
                savedUser.getEmail(), savedUser.getRole());

        publishUserRegisteredEvent(savedUser);

        return "Registration successful. Welcome, "
                + savedUser.getFullName() + "!";
    }

    public AuthResponse login(LoginRequest request) {

        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getEmail(),
                    request.getPassword()
                )
            );
        } catch (BadCredentialsException e) {
            throw new AuthException("Invalid email or password");
        } catch (DisabledException e) {
            throw new AuthException("Your account has been deactivated. "
                    + "Please contact HR.");
        }

        User user = userRepository
                .findByEmailAndIsActiveTrue(request.getEmail())
                .orElseThrow(() ->
                    new AuthException("User not found or account inactive"));

        String token = jwtUtil.generateToken(user);

        log.info("User logged in: {} with role {}",
                user.getEmail(), user.getRole());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .role(user.getRole().name())
                .employeeId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .expiresIn(jwtExpiration)
                .build();
    }

    private void publishUserRegisteredEvent(User user) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType",    "USER_REGISTERED");
            event.put("userId",       user.getId());
            event.put("email",        user.getEmail());
            event.put("employeeCode", user.getEmployeeCode());
            event.put("role",         user.getRole().name());
            event.put("timestamp",    java.time.LocalDateTime.now().toString());

            rabbitTemplate.convertAndSend(
                "tms.exchange",
                "user.registered",
                event
            );

            log.info("Published USER_REGISTERED event for: {}",
                    user.getEmail());

        } catch (Exception e) {
            log.error("Failed to publish USER_REGISTERED event for {}: {}",
                    user.getEmail(), e.getMessage());
        }
    }
}