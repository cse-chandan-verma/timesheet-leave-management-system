package com.application.notification.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService — Unit Tests")
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @Test
    @DisplayName("sendEmail() — success")
    void sendEmail_Success() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "test@tms.com");
        emailService.sendEmail("user@test.com", "Subject", "Body");
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("sendRegistrationEmail() — success")
    void sendRegistrationEmail_Success() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "test@tms.com");
        emailService.sendRegistrationEmail("user@test.com", "John");
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("sendProfileUpdateEmail() — success")
    void sendProfileUpdateEmail_Success() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "test@tms.com");
        emailService.sendProfileUpdateEmail("user@test.com", "John");
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("sendPasswordChangeEmail() — success")
    void sendPasswordChangeEmail_Success() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "test@tms.com");
        emailService.sendPasswordChangeEmail("user@test.com");
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("sendLeaveStatusEmail() — success")
    void sendLeaveStatusEmail_Success() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "test@tms.com");
        emailService.sendLeaveStatusEmail("user@test.com", "John", "APPROVED", "Remarks", "AL", "2024-01-01", "2024-01-02");
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("sendTimesheetStatusEmail() — success")
    void sendTimesheetStatusEmail_Success() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "test@tms.com");
        emailService.sendTimesheetStatusEmail("user@test.com", "John", "APPROVED", "Good", "2024-01-01");
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("sendRoleUpdateEmail() — success")
    void sendRoleUpdateEmail_Success() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "test@tms.com");
        emailService.sendRoleUpdateEmail("user@test.com", "John", "EMPLOYEE", "MANAGER");
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("sendEmail() — failure: logged but not thrown")
    void sendEmail_Failure() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "test@tms.com");
        doThrow(new RuntimeException("Mail server down")).when(mailSender).send(any(SimpleMailMessage.class));
        emailService.sendEmail("user@test.com", "Fail", "Fail");
        verify(mailSender).send(any(SimpleMailMessage.class));
    }
}
