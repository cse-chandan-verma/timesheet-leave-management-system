package com.application.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    public void sendRegistrationEmail(String to, String name) {
        String subject = "Welcome to TMS!";
        String body = "Hello " + name + ",\n\n" +
                "You have successfully registered on the Timesheet & Leave Management System.\n" +
                "You can now log in and manage your timesheets and leaves.\n\n" +
                "Best Regards,\n" +
                "Chandan\n" +
                "TMS Team";
        sendEmail(to, subject, body);
    }

    public void sendProfileUpdateEmail(String to, String name) {
        String subject = "Profile Updated Successfully";
        String body = "Hello " + name + ",\n\n" +
                "Your profile details have been updated successfully.\n" +
                "If this was not you, please contact HR immediately.\n\n" +
                "Best Regards,\n" +
                "Chandan\n" +
                "TMS Team";
        sendEmail(to, subject, body);
    }

    public void sendPasswordChangeEmail(String to) {
        String subject = "Password Changed Successfully";
        String body = "Hello,\n\n" +
                "Your password has been changed successfully.\n" +
                "If this was not you, please contact HR or reset your password immediately.\n\n" +
                "Best Regards,\n" +
                "Chandan\n" +
                "TMS Team";
        sendEmail(to, subject, body);
    }

    public void sendLeaveStatusEmail(String to, String name, String status, String remarks, String leaveType,
            String fromDate, String toDate) {
        String subject = "Leave Application " + status;
        String body = "Hello " + name + ",\n\n" +
                "Your leave application for " + leaveType + " (" + fromDate + " to " + toDate + ") has been "
                + status.toUpperCase() + ".\n\n" +
                "Remarks: " + (remarks != null ? remarks : "N/A") + "\n\n" +
                "Best Regards,\n" +
                "Chandan\n" +
                "TMS Team";
        sendEmail(to, subject, body);
    }

    public void sendTimesheetStatusEmail(String to, String name, String status, String remarks, String weekStart) {
        String subject = "Timesheet " + status;
        String body = "Hello " + name + ",\n\n" +
                "Your timesheet for the week starting " + weekStart + " has been " + status.toUpperCase() + ".\n\n" +
                "Remarks: " + (remarks != null ? remarks : "N/A") + "\n\n" +
                "Best Regards,\n" +
                "Chandan\n" +
                "TMS Team";
        sendEmail(to, subject, body);
    }

    public void sendRoleUpdateEmail(String to, String name, String oldRole, String newRole) {
        String subject = "Role Updated - TMS";
        String action = newRole.contains("ADMIN") || (newRole.contains("MANAGER") && !oldRole.contains("ADMIN"))
                ? "PROMOTED"
                : "UPDATED";
        String body = "Hello " + name + ",\n\n" +
                "Your role in the Timesheet & Leave Management System has been " + action + ".\n" +
                "Old Role: " + oldRole + "\n" +
                "New Role: " + newRole + "\n\n" +
                "Please log out and log in again to see the changes.\n\n" +
                "Best Regards,\n" +
                "Chandan\n" +
                "TMS Team";
        sendEmail(to, subject, body);
    }
}