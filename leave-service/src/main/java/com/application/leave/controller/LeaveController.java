package com.application.leave.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.application.leave.dto.LeaveBalanceResponse;
import com.application.leave.dto.LeaveRequestDto;
import com.application.leave.dto.LeaveResponse;
import com.application.leave.entity.LeaveType;
import com.application.leave.service.LeaveService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/leave")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    // ── Apply leave ──────────────────────────────────────────
    @PostMapping("/apply")
    public ResponseEntity<LeaveResponse> applyLeave(
            @RequestHeader("X-User-Id")    Long employeeId,
            @RequestHeader("X-User-Email") String employeeName,
            @Valid @RequestBody LeaveRequestDto request) {

        LeaveResponse response = leaveService
                .applyLeave(employeeId, employeeName, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── Get leave balance ────────────────────────────────────
    @GetMapping("/balance")
    public ResponseEntity<List<LeaveBalanceResponse>> getBalance(
            @RequestHeader("X-User-Id") Long employeeId) {

        return ResponseEntity.ok(
                leaveService.getLeaveBalance(employeeId));
    }

    // ── Get leave history ────────────────────────────────────
    @GetMapping("/history")
    public ResponseEntity<List<LeaveResponse>> getHistory(
            @RequestHeader("X-User-Id") Long employeeId) {

        return ResponseEntity.ok(
                leaveService.getLeaveHistory(employeeId));
    }

    // ── Cancel leave ─────────────────────────────────────────
    @PutMapping("/cancel/{id}")
    public ResponseEntity<String> cancelLeave(
            @RequestHeader("X-User-Id") Long employeeId,
            @PathVariable Long id) {

        return ResponseEntity.ok(
                leaveService.cancelLeave(employeeId, id));
    }

    // ── Get leave types (dropdown) ───────────────────────────
    @GetMapping("/types")
    public ResponseEntity<List<LeaveType>> getLeaveTypes() {
        return ResponseEntity.ok(leaveService.getLeaveTypes());
    }
}
