package com.application.timesheet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.application.timesheet.dto.SubmitTimesheetRequest;
import com.application.timesheet.dto.TimesheetEntryRequest;
import com.application.timesheet.dto.TimesheetEntryResponse;
import com.application.timesheet.dto.WeeklyTimesheetResponse;
import com.application.timesheet.entity.Project;
import com.application.timesheet.service.TimesheetService;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/timesheet")
@RequiredArgsConstructor
public class TimesheetController {

    private final TimesheetService timesheetService;

    // ── Add daily entry ──────────────────────────────────────
    @PostMapping("/entries")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<TimesheetEntryResponse> addEntry(
            @RequestHeader("X-User-Id") Long employeeId,
            @RequestHeader("X-User-Email") String employeeName,
            @Valid @RequestBody TimesheetEntryRequest request) {

        // X-User-Id and X-User-Email are added by the Gateway
        // after validating the JWT token
        TimesheetEntryResponse response =
                timesheetService.addEntry(employeeId, employeeName, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── Get weekly timesheet ─────────────────────────────────
    @GetMapping("/weeks/{weekStart}")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<WeeklyTimesheetResponse> getWeeklyTimesheet(
            @RequestHeader("X-User-Id") Long employeeId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                LocalDate weekStart) {

        return ResponseEntity.ok(
                timesheetService.getWeeklyTimesheet(employeeId, weekStart));
    }

    // ── Submit weekly timesheet ──────────────────────────────
    @PostMapping("/weeks/submit")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<String> submitTimesheet(
            @RequestHeader("X-User-Id") Long employeeId,
            @RequestHeader("X-User-Email") String employeeName,
            @Valid @RequestBody SubmitTimesheetRequest request) {

        String message = timesheetService.submitTimesheet(
                employeeId, employeeName, request);
        return ResponseEntity.ok(message);
    }

    // ── Get all timesheets (history) ─────────────────────────
    @GetMapping("/history")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<List<WeeklyTimesheetResponse>> getHistory(
            @RequestHeader("X-User-Id") Long employeeId) {

        return ResponseEntity.ok(
                timesheetService.getAllTimesheets(employeeId));
    }

    // ── Get active projects (for dropdown) ───────────────────
    @GetMapping("/projects")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<List<Project>> getProjects() {
        return ResponseEntity.ok(timesheetService.getActiveProjects());
    }
}