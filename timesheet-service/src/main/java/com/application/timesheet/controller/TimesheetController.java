package com.application.timesheet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.application.timesheet.dto.ApproveRejectRequest;
import com.application.timesheet.dto.ProjectResponse;
import com.application.timesheet.dto.SubmitTimesheetRequest;
import com.application.timesheet.dto.TimesheetEntryRequest;
import com.application.timesheet.dto.TimesheetEntryResponse;
import com.application.timesheet.dto.UpdateEntryRequest;
import com.application.timesheet.dto.WeeklyTimesheetResponse;
import com.application.timesheet.service.TimesheetService;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/timesheet")
@RequiredArgsConstructor
public class TimesheetController {

    private final TimesheetService timesheetService;

    // ── Employee: Entry Management ────────────────────────────────────────────

    /**
     * POST /timesheet/entries
     * Add a new daily work entry to the current week's timesheet (auto-created if missing).
     * Rejects: weekends, future dates, inactive projects, duplicates, daily hour overflows.
     */
    @PostMapping("/entries")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER')")
    public ResponseEntity<TimesheetEntryResponse> addEntry(
            @RequestHeader("X-User-Id")    Long   employeeId,
            @RequestHeader("X-User-Email") String employeeName,
            @Valid @RequestBody TimesheetEntryRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(timesheetService.addEntry(employeeId, employeeName, request));
    }

    /**
     * PUT /timesheet/entries/{entryId}
     * Update hours/summary of an existing entry. Only allowed on DRAFT or REJECTED timesheets.
     * Validates ownership and daily hour limits after the update.
     */
    @PutMapping("/entries/{entryId}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER')")
    public ResponseEntity<TimesheetEntryResponse> updateEntry(
            @RequestHeader("X-User-Id") Long employeeId,
            @PathVariable Long entryId,
            @Valid @RequestBody UpdateEntryRequest request) {

        return ResponseEntity.ok(timesheetService.updateEntry(entryId, employeeId, request));
    }

    /**
     * DELETE /timesheet/entries/{entryId}
     * Remove an entry from a DRAFT or REJECTED timesheet.
     * Total hours on the parent timesheet are automatically recalculated.
     */
    @DeleteMapping("/entries/{entryId}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER')")
    public ResponseEntity<Void> deleteEntry(
            @RequestHeader("X-User-Id") Long employeeId,
            @PathVariable Long entryId) {

        timesheetService.deleteEntry(entryId, employeeId);
        return ResponseEntity.noContent().build();
    }

    // ── Employee: Timesheet Management ───────────────────────────────────────

    /**
     * GET /timesheet/weeks/{weekStart}
     * Retrieve the full weekly timesheet for any date in the target week.
     * The date is normalised to the Monday of that week automatically.
     */
    @GetMapping("/weeks/{weekStart}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER')")
    public ResponseEntity<WeeklyTimesheetResponse> getWeeklyTimesheet(
            @RequestHeader("X-User-Id") Long employeeId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {

        return ResponseEntity.ok(timesheetService.getWeeklyTimesheet(employeeId, weekStart));
    }

    /**
     * POST /timesheet/weeks/submit
     * Submit a DRAFT or REJECTED timesheet for manager review.
     * Validates all Mon–Fri have entries and total hours ≤ 60.
     */
    @PostMapping("/weeks/submit")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER')")
    public ResponseEntity<String> submitTimesheet(
            @RequestHeader("X-User-Id")    Long   employeeId,
            @RequestHeader("X-User-Email") String employeeEmail,
            @Valid @RequestBody SubmitTimesheetRequest request) {

        return ResponseEntity.ok(
                timesheetService.submitTimesheet(employeeId, employeeEmail, request));
    }

    /**
     * POST /timesheet/weeks/recall
     * Pull back a SUBMITTED timesheet to DRAFT before it is reviewed.
     * Useful for correcting mistakes after submission.
     */
    @PostMapping("/weeks/recall")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER')")
    public ResponseEntity<String> recallTimesheet(
            @RequestHeader("X-User-Id") Long employeeId,
            @Valid @RequestBody SubmitTimesheetRequest request) {

        return ResponseEntity.ok(timesheetService.recallTimesheet(employeeId, request));
    }

    /**
     * GET /timesheet/history
     * Get all timesheets for the calling employee, ordered newest first.
     */
    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER')")
    public ResponseEntity<List<WeeklyTimesheetResponse>> getHistory(
            @RequestHeader("X-User-Id") Long employeeId) {

        return ResponseEntity.ok(timesheetService.getAllTimesheets(employeeId));
    }

    /**
     * GET /timesheet/projects
     * List all active projects available for timesheet entries (for dropdowns).
     * Returns a clean DTO, not the raw entity.
     */
    @GetMapping("/projects")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER')")
    public ResponseEntity<List<ProjectResponse>> getProjects() {
        return ResponseEntity.ok(timesheetService.getActiveProjects());
    }

    // ── Admin / Manager: Approval Workflow ───────────────────────────────────

    /**
     * GET /timesheet/admin/submitted
     * Fetch every timesheet currently pending review (status = SUBMITTED).
     * Includes employeeId and employeeName so managers know whose sheet it is.
     */
    @GetMapping("/admin/submitted")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<WeeklyTimesheetResponse>> getSubmittedTimesheets() {
        return ResponseEntity.ok(timesheetService.getSubmittedTimesheets());
    }

    /**
     * PUT /timesheet/admin/approve/{timesheetId}
     * Approve a submitted timesheet. An optional comment can be added.
     * Only timesheets in SUBMITTED status can be approved.
     */
    @PutMapping("/admin/approve/{timesheetId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<String> approveTimesheet(
            @PathVariable Long timesheetId,
            @Valid @RequestBody ApproveRejectRequest request) {

        return ResponseEntity.ok(timesheetService.approveTimesheet(timesheetId, request));
    }

    /**
     * PUT /timesheet/admin/reject/{timesheetId}
     * Reject a submitted timesheet. A comment is MANDATORY so the employee
     * knows what to fix before resubmitting.
     */
    @PutMapping("/admin/reject/{timesheetId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<String> rejectTimesheet(
            @PathVariable Long timesheetId,
            @Valid @RequestBody ApproveRejectRequest request) {

        return ResponseEntity.ok(timesheetService.rejectTimesheet(timesheetId, request));
    }
}