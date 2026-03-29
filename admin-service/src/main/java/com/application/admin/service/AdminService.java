package com.application.admin.service;

import java.util.List;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.application.admin.dto.ApprovalRequestDTO;
import com.application.admin.dto.LeaveResponseDTO;
import com.application.admin.dto.TimesheetResponseDTO;
import com.application.admin.entity.AdminNotification;
import com.application.admin.exception.AdminException;
import com.application.admin.feign.LeaveClient;
import com.application.admin.feign.TimesheetClient;
import com.application.admin.repository.AdminNotificationRepository;

/**
 * Service to aggregate administrative tasks.
 * Actively communicates with timesheet and leave microservices via Feign clients.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final TimesheetClient timesheetClient;
    private final LeaveClient     leaveClient;
    private final AdminNotificationRepository notificationRepository;

    // ── Timesheet Operations ─────────────────────────────────────────────

    public List<TimesheetResponseDTO> getPendingTimesheets() {
        log.info("Fetching pending timesheets from timesheet-service...");
        return timesheetClient.getPendingTimesheets();
    }

    public String approveTimesheet(Long timesheetId, String comment) {
        log.info("Approving timesheet: {}", timesheetId);
        ApprovalRequestDTO request = new ApprovalRequestDTO(comment);
        return timesheetClient.approveTimesheet(timesheetId, request);
    }

    public String rejectTimesheet(Long timesheetId, String comment) {
        log.info("Rejecting timesheet: {}", timesheetId);
        if (comment == null || comment.isBlank()) {
            throw new AdminException("Rejection comment is mandatory.");
        }
        ApprovalRequestDTO request = new ApprovalRequestDTO(comment);
        return timesheetClient.rejectTimesheet(timesheetId, request);
    }

    // ── Leave Operations ────────────────────────────────────────────────

    public List<LeaveResponseDTO> getPendingLeaveRequests() {
        log.info("Fetching pending leave requests from leave-service...");
        return leaveClient.getPendingLeaveRequests();
    }

    public String approveLeave(Long leaveId, Long employeeId, String comment) {
        log.info("Approving leave request: {}", leaveId);
        ApprovalRequestDTO request = new ApprovalRequestDTO(comment);
        request.setEmployeeId(employeeId);
        return leaveClient.approveLeave(leaveId, request);
    }

    public String rejectLeave(Long leaveId, Long employeeId, String comment) {
        log.info("Rejecting leave request: {}", leaveId);
        if (comment == null || comment.isBlank()) {
            throw new AdminException("Rejection comment is mandatory.");
        }
        ApprovalRequestDTO request = new ApprovalRequestDTO(comment);
        request.setEmployeeId(employeeId);
        return leaveClient.rejectLeave(leaveId, request);
    }

    // ── Notification Operations ──────────────────────────────────────────

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