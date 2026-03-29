package com.application.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.application.admin.dto.ApiResponseDTO;
import com.application.admin.dto.ApprovalRequestDTO;
import com.application.admin.dto.LeaveResponseDTO;
import com.application.admin.dto.TimesheetResponseDTO;
import com.application.admin.entity.AdminNotification;
import com.application.admin.service.AdminService;

import jakarta.validation.Valid;
import java.util.List;

/**
 * Aggregator controller for admin tasks.
 * Coordinates between management functions across multiple services.
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin & Approval Aggregator", description = "Unified management portal for timesheets and leaves")
public class AdminController {

    private final AdminService adminService;

    // ── Timesheet Approval ─────────────────────────────────────────────

    @GetMapping("/timesheets/pending")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Get all pending timesheets across departments")
    public ResponseEntity<ApiResponseDTO<List<TimesheetResponseDTO>>> getPendingTimesheets() {

        List<TimesheetResponseDTO> result = adminService.getPendingTimesheets();
        return ResponseEntity.ok(
                new ApiResponseDTO<>("Pending timesheets fetched.", result));
    }

    @PutMapping("/timesheets/{id}/approve")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Approve a timesheet via proxy")
    public ResponseEntity<ApiResponseDTO<String>> approveTimesheet(
            @PathVariable Long id,
            @RequestBody(required = false) ApprovalRequestDTO request) {

        String comment = (request != null) ? request.getComment() : null;
        String message = adminService.approveTimesheet(id, comment);

        return ResponseEntity.ok(
                new ApiResponseDTO<>(message, null));
    }

    @PutMapping("/timesheets/{id}/reject")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Reject a timesheet via proxy")
    public ResponseEntity<ApiResponseDTO<String>> rejectTimesheet(
            @PathVariable Long id,
            @Valid @RequestBody ApprovalRequestDTO request) {

        String message = adminService.rejectTimesheet(id, request.getComment());

        return ResponseEntity.ok(
                new ApiResponseDTO<>(message, null));
    }

    // ── Leave Approval ────────────────────────────────────────────────

    @GetMapping("/leaves/pending")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Get all pending leave requests across departments")
    public ResponseEntity<ApiResponseDTO<List<LeaveResponseDTO>>> getPendingLeaves() {

        List<LeaveResponseDTO> result = adminService.getPendingLeaveRequests();
        return ResponseEntity.ok(
                new ApiResponseDTO<>("Pending leave requests fetched.", result));
    }

    @PutMapping("/leaves/{id}/approve")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Approve a leave request via proxy")
    public ResponseEntity<ApiResponseDTO<String>> approveLeave(
            @PathVariable Long id,
            @RequestBody(required = false) ApprovalRequestDTO request) {

        String comment = (request != null) ? request.getComment() : null;
        Long employeeId = (request != null) ? request.getEmployeeId() : null;
        String message = adminService.approveLeave(id, employeeId, comment);

        return ResponseEntity.ok(
                new ApiResponseDTO<>(message, null));
    }

    @PutMapping("/leaves/{id}/reject")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Reject a leave request via proxy")
    public ResponseEntity<ApiResponseDTO<String>> rejectLeave(
            @PathVariable Long id,
            @Valid @RequestBody ApprovalRequestDTO request) {

        String message = adminService.rejectLeave(id, request.getEmployeeId(), request.getComment());

        return ResponseEntity.ok(
                new ApiResponseDTO<>(message, null));
    }

    // ── Notifications ──────────────────────────────────────────────────

    @GetMapping("/notifications")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all audit notifications")
    public ResponseEntity<ApiResponseDTO<List<AdminNotification>>> getAllNotifications() {
        return ResponseEntity.ok(
                new ApiResponseDTO<>("Audit logs fetched.",
                        adminService.getAllNotifications()));
    }

    @GetMapping("/notifications/type/{type}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Filter notifications by type")
    public ResponseEntity<ApiResponseDTO<List<AdminNotification>>> getByType(
            @PathVariable String type) {
        return ResponseEntity.ok(
                new ApiResponseDTO<>("Filtered logs fetched.",
                        adminService.getNotificationsByType(type)));
    }

    @GetMapping("/notifications/user/{email}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Filter notifications by employee email")
    public ResponseEntity<ApiResponseDTO<List<AdminNotification>>> getByUser(
            @PathVariable String email) {
        return ResponseEntity.ok(
                new ApiResponseDTO<>("User-specific logs fetched.",
                        adminService.getNotificationsByUser(email)));
    }
}
