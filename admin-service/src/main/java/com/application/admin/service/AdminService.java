package com.application.admin.service;

import org.springframework.stereotype.Service;

import com.application.admin.dto.LeaveResponseDTO;
import com.application.admin.dto.TimesheetResponseDTO;
import com.application.admin.entity.AdminNotification;
import com.application.admin.feign.LeaveClient;
import com.application.admin.feign.TimesheetClient;
import com.application.admin.repository.AdminNotificationRepository;

import java.util.List;

@Service
public class AdminService {

    private final TimesheetClient timesheetClient;
    private final LeaveClient leaveClient;
    private final AdminNotificationRepository notificationRepository;

    public AdminService(TimesheetClient timesheetClient,
                        LeaveClient leaveClient,
                        AdminNotificationRepository notificationRepository) {
        this.timesheetClient       = timesheetClient;
        this.leaveClient           = leaveClient;
        this.notificationRepository = notificationRepository;
    }

    // ── Timesheet operations ──────────────────────────────────────────────

    public List<TimesheetResponseDTO> getPendingTimesheets(
            String email, String role) {
        return timesheetClient.getPendingTimesheets(email, role);
    }

    public TimesheetResponseDTO approveTimesheet(
            Long id, String email, String role) {
        return timesheetClient.approveTimesheet(id, email, role);
    }

    public TimesheetResponseDTO rejectTimesheet(
            Long id, String remarks, String email, String role) {
        if (remarks == null || remarks.isBlank()) {
            throw new RuntimeException(
                    "Remarks are mandatory when rejecting a timesheet.");
        }
        return timesheetClient.rejectTimesheet(id, remarks, email, role);
    }

    // ── Leave operations ──────────────────────────────────────────────────

    public List<LeaveResponseDTO> getPendingLeaveRequests(
            String email, String role) {
        return leaveClient.getPendingLeaveRequests(email, role);
    }

    public LeaveResponseDTO approveLeave(
            Long id, String email, String role) {
        return leaveClient.approveLeave(id, email, role);
    }

    public LeaveResponseDTO rejectLeave(
            Long id, String remarks, String email, String role) {
        if (remarks == null || remarks.isBlank()) {
            throw new RuntimeException(
                    "Remarks are mandatory when rejecting a leave request.");
        }
        return leaveClient.rejectLeave(id, remarks, email, role);
    }

    // ── Notification operations ───────────────────────────────────────────

    public List<AdminNotification> getAllNotifications() {
        return notificationRepository.findAll();
    }

    public List<AdminNotification> getNotificationsByType(String type) {
        return notificationRepository
                .findByEventTypeOrderByReceivedAtDesc(type.toUpperCase());
    }

    public List<AdminNotification> getNotificationsByUser(String userEmail) {
        return notificationRepository
                .findByUserEmailOrderByReceivedAtDesc(userEmail);
    }
}