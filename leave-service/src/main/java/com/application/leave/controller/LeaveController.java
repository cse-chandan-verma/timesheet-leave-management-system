package com.application.leave.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.application.leave.dto.ApproveRejectLeaveRequest;
import com.application.leave.dto.HolidayRequest;
import com.application.leave.dto.HolidayResponse;
import com.application.leave.dto.LeaveBalanceResponse;
import com.application.leave.dto.LeaveRequestDto;
import com.application.leave.dto.LeaveResponse;
import com.application.leave.dto.LeaveTypeResponse;
import com.application.leave.service.LeaveService;

import io.swagger.v3.oas.annotations.Operation;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/leave")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    // ── Employee: Core Leave Operations 

    @PostMapping("/apply")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER')")
    @Operation(summary = "Apply Leave",
	description = "Only Employee and Manager can apply the leave")
    public ResponseEntity<LeaveResponse> applyLeave(
            @RequestHeader("X-User-Id")    Long   employeeId,
            @RequestHeader("X-User-Email") String employeeEmail,
            @Valid @RequestBody LeaveRequestDto request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(leaveService.applyLeave(employeeId, employeeEmail, request));
    }

    
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER')")
    @Operation(summary = "Get Leave By Id")
    public ResponseEntity<LeaveResponse> getLeaveById(
            @RequestHeader("X-User-Id") Long employeeId,
            @PathVariable Long id) {

        return ResponseEntity.ok(leaveService.getLeaveById(employeeId, id));
    }

    @GetMapping("/balance")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER')")
    @Operation(summary = "Get Leave Balance")
    public ResponseEntity<List<LeaveBalanceResponse>> getBalance(
            @RequestHeader("X-User-Id") Long employeeId) {

        return ResponseEntity.ok(leaveService.getLeaveBalance(employeeId));
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER')")
    @Operation(summary = "Get Leave History")
    public ResponseEntity<List<LeaveResponse>> getHistory(
            @RequestHeader("X-User-Id") Long employeeId) {

        return ResponseEntity.ok(leaveService.getLeaveHistory(employeeId));
    }

    
    @PutMapping("/cancel/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER')")
    @Operation(summary = "Cancel Leave By Id",
    description = "Cancel a SUBMITTED leave freely or Cancel an APPROVED leave only if it hasn't started yet — balance is restored")
    public ResponseEntity<String> cancelLeave(
            @RequestHeader("X-User-Id") Long employeeId,
            @PathVariable Long id) {

        return ResponseEntity.ok(leaveService.cancelLeave(employeeId, id));
    }

    
    @GetMapping("/types")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Get Leave Types", description = "List all the leave types")
    public ResponseEntity<List<LeaveTypeResponse>> getLeaveTypes() {
        return ResponseEntity.ok(leaveService.getLeaveTypes());
    }

    // ── Admin / Manager: Approval Workflow
    
    @GetMapping("/admin/pending")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Get Pending Leave", description = "Fetch all leave requests currently in SUBMITTED status pending revie")
    public ResponseEntity<List<LeaveResponse>> getPendingLeaves() {
        return ResponseEntity.ok(leaveService.getAllSubmittedLeaves());
    }

    
    @PutMapping("/admin/approve/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Put Approve Leave By Admin", description = "Approve a submitted leave request")
    public ResponseEntity<String> approveLeave(
            @PathVariable Long id,
            @Valid @RequestBody ApproveRejectLeaveRequest request) {

        return ResponseEntity.ok(leaveService.approveLeave(id, request));
    }

    
    @PutMapping("/admin/reject/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Put Reject Leave By Id")
    public ResponseEntity<String> rejectLeave(
            @PathVariable Long id,
            @Valid @RequestBody ApproveRejectLeaveRequest request) {

        return ResponseEntity.ok(leaveService.rejectLeave(id, request));
    }

    // ── Admin: Holiday Management 

    
    @PostMapping("/admin/holidays")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Add Holiday(Admin)", description = "Register a new public holiday. Affects working-day calculation for all leave requests") 
    public ResponseEntity<HolidayResponse> addHoliday(
            @Valid @RequestBody HolidayRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(leaveService.addHoliday(request));
    }

    
    @GetMapping("/admin/holidays")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Get Leave Holidays(Admin)", description = "List all public holidays for a given year, ordered by date")
    public ResponseEntity<List<HolidayResponse>> getHolidays(
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().getYear()}") int year) {

        return ResponseEntity.ok(leaveService.getHolidaysByYear(year));
    }

    
    @DeleteMapping("/admin/holidays/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary  = "Delete leave Holidays",description = "Remove a public holiday by ID")
    public ResponseEntity<String> deleteHoliday(@PathVariable Long id) {
        return ResponseEntity.ok(leaveService.deleteHoliday(id));
    }
}
