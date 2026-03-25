package com.application.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.application.admin.dto.ApiResponseDTO;
import com.application.admin.dto.ApprovalRequestDTO;
import com.application.admin.dto.LeaveResponseDTO;
import com.application.admin.dto.TimesheetResponseDTO;
import com.application.admin.entity.AdminNotification;
import com.application.admin.service.AdminService;

import java.util.List;

@RestController
@RequestMapping("/admin")
@Tag(name = "Admin & Approval", description = "Timesheet and Leave approval APIs")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // ── Helper to extract role from Authentication ────────────────────────

    private String extractRole(Authentication auth) {
        return auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("UNKNOWN");
    }

    // ── Timesheet Approval ────────────────────────────────────────────────

    @GetMapping("/timesheets/pending")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Get all pending timesheets")
    public ResponseEntity<ApiResponseDTO<List<TimesheetResponseDTO>>> getPendingTimesheets(
            Authentication auth) {

        List<TimesheetResponseDTO> list =
                adminService.getPendingTimesheets(
                        auth.getName(), extractRole(auth));

        return ResponseEntity.ok(
                new ApiResponseDTO<>("Pending timesheets fetched.", list));
    }

    @PutMapping("/timesheets/{id}/approve")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Approve a timesheet")
    public ResponseEntity<ApiResponseDTO<TimesheetResponseDTO>> approveTimesheet(
            @PathVariable Long id,
            Authentication auth) {

        TimesheetResponseDTO result =
                adminService.approveTimesheet(
                        id, auth.getName(), extractRole(auth));

        return ResponseEntity.ok(
                new ApiResponseDTO<>("Timesheet approved successfully.", result));
    }

    @PutMapping("/timesheets/{id}/reject")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Reject a timesheet")
    public ResponseEntity<ApiResponseDTO<TimesheetResponseDTO>> rejectTimesheet(
            @PathVariable Long id,
            @RequestBody ApprovalRequestDTO request,
            Authentication auth) {

        TimesheetResponseDTO result =
                adminService.rejectTimesheet(
                        id, request.getRemarks(),
                        auth.getName(), extractRole(auth));

        return ResponseEntity.ok(
                new ApiResponseDTO<>("Timesheet rejected.", result));
    }

    // ── Leave Approval ────────────────────────────────────────────────────

    @GetMapping("/leaves/pending")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Get all pending leave requests")
    public ResponseEntity<ApiResponseDTO<List<LeaveResponseDTO>>> getPendingLeaves(
            Authentication auth) {

        List<LeaveResponseDTO> list =
                adminService.getPendingLeaveRequests(
                        auth.getName(), extractRole(auth));

        return ResponseEntity.ok(
                new ApiResponseDTO<>("Pending leave requests fetched.", list));
    }

    @PutMapping("/leaves/{id}/approve")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Approve a leave request")
    public ResponseEntity<ApiResponseDTO<LeaveResponseDTO>> approveLeave(
            @PathVariable Long id,
            Authentication auth) {

        LeaveResponseDTO result =
                adminService.approveLeave(
                        id, auth.getName(), extractRole(auth));

        return ResponseEntity.ok(
                new ApiResponseDTO<>("Leave approved successfully.", result));
    }

    @PutMapping("/leaves/{id}/reject")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Reject a leave request")
    public ResponseEntity<ApiResponseDTO<LeaveResponseDTO>> rejectLeave(
            @PathVariable Long id,
            @RequestBody ApprovalRequestDTO request,
            Authentication auth) {

        LeaveResponseDTO result =
                adminService.rejectLeave(
                        id, request.getRemarks(),
                        auth.getName(), extractRole(auth));

        return ResponseEntity.ok(
                new ApiResponseDTO<>("Leave rejected.", result));
    }

    // ── Notifications ─────────────────────────────────────────────────────

    @GetMapping("/notifications")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all admin notifications")
    public ResponseEntity<ApiResponseDTO<List<AdminNotification>>> getAllNotifications() {
        return ResponseEntity.ok(
                new ApiResponseDTO<>("Notifications fetched.",
                        adminService.getAllNotifications()));
    }

    @GetMapping("/notifications/type/{type}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get notifications by type (TIMESHEET or LEAVE)")
    public ResponseEntity<ApiResponseDTO<List<AdminNotification>>> getByType(
            @PathVariable String type) {
        return ResponseEntity.ok(
                new ApiResponseDTO<>("Notifications fetched.",
                        adminService.getNotificationsByType(type)));
    }

    @GetMapping("/notifications/user/{email}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Get notifications for a specific user")
    public ResponseEntity<ApiResponseDTO<List<AdminNotification>>> getByUser(
            @PathVariable String email) {
        return ResponseEntity.ok(
                new ApiResponseDTO<>("Notifications fetched.",
                        adminService.getNotificationsByUser(email)));
    }
}
