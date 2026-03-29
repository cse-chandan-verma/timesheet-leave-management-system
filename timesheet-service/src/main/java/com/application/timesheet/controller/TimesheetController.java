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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/timesheet")
@RequiredArgsConstructor
@Tag(name = "Timesheet Controller", description = "Operations related to employee timesheets and work entries")
public class TimesheetController {

    private final TimesheetService timesheetService;

    // ── Employee: Entry Management ────────────────────────────────────────────

    @PostMapping("/entries")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER')")
    @Operation(summary = "Add Timesheet Entry", description = "Create a new daily work entry for the current week. Automatically creates a weekly timesheet if one doesn't exist.")
    public ResponseEntity<TimesheetEntryResponse> addEntry(
            @RequestHeader("X-User-Id") Long employeeId,
            @RequestHeader("X-User-Email") String employeeName,
            @Valid @RequestBody TimesheetEntryRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(timesheetService.addEntry(employeeId, employeeName, request));
    }

    @PutMapping("/entries/{entryId}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER')")
    @Operation(summary = "Update Timesheet Entry(EMPLOYEE,MANAGER)", description = "Modify an existing entry's hours or summary. Only valid for DRAFT or REJECTED timesheets.")
    public ResponseEntity<TimesheetEntryResponse> updateEntry(
            @RequestHeader("X-User-Id") Long employeeId,
            @PathVariable Long entryId,
            @Valid @RequestBody UpdateEntryRequest request) {

        return ResponseEntity.ok(timesheetService.updateEntry(entryId, employeeId, request));
    }

    @DeleteMapping("/entries/{entryId}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER')")
    @Operation(summary = "Delete Timesheet Entry(EMPLOYEE,MANAGER)", description = "Remove an entry from a DRAFT or REJECTED timesheet. Total hours on the parent timesheet are automatically recalculated.")
    public ResponseEntity<String> deleteEntry(
            @RequestHeader("X-User-Id") Long employeeId,
            @PathVariable Long entryId) {

        timesheetService.deleteEntry(entryId, employeeId);
        return ResponseEntity.ok("Timesheet entry ID " + entryId + " has been successfully deleted.");
    }

    // ── Employee: Timesheet Management ───────────────────────────────────────

    @GetMapping("/weeks/{weekStart}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER')")
    @Operation(summary = "Get Weekly Timesheet(EMPLOYEE,MANAGER)", description = "Retrieve the full weekly timesheet for any date in the target week. The date is normalised to the Monday of that week automatically.")
    public ResponseEntity<WeeklyTimesheetResponse> getWeeklyTimesheet(
            @RequestHeader("X-User-Id") Long employeeId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {

        return ResponseEntity.ok(timesheetService.getWeeklyTimesheet(employeeId, weekStart));
    }

    @PostMapping("/weeks/submit")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER')")
    @Operation(summary = "Submit Timesheet(EMPLOYEE,MANAGER)", description = "Submit a DRAFT or REJECTED timesheet for manager review. Validates all Mon–Fri have entries and total hours ≤ 60.")
    public ResponseEntity<String> submitTimesheet(
            @RequestHeader("X-User-Id") Long employeeId,
            @RequestHeader("X-User-Email") String employeeEmail,
            @Valid @RequestBody SubmitTimesheetRequest request) {

        return ResponseEntity.ok(
                timesheetService.submitTimesheet(employeeId, employeeEmail, request));
    }

    @PostMapping("/weeks/recall")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER')")
    @Operation(summary = "Recall Weekly Timesheet(EMPLOYEE,MANAGER)", description = "Pull back a SUBMITTED timesheet to DRAFT before it is reviewed. Useful for correcting mistakes after submission.")
    public ResponseEntity<String> recallTimesheet(
            @RequestHeader("X-User-Id") Long employeeId,
            @Valid @RequestBody SubmitTimesheetRequest request) {

        return ResponseEntity.ok(timesheetService.recallTimesheet(employeeId, request));
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER')")
    @Operation(summary = "Get Weekly Timesheet History(EMPLOYEE,MANAGER)", description = "Get all timesheets for the calling employee, ordered newest first.")
    public ResponseEntity<List<WeeklyTimesheetResponse>> getHistory(
            @RequestHeader("X-User-Id") Long employeeId) {

        return ResponseEntity.ok(timesheetService.getAllTimesheets(employeeId));
    }

    @GetMapping("/projects")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER')")
    @Operation(summary = "Get Active Projects(EMPLOYEE,MANAGER)", description = "List all active projects available for timesheet entries (for dropdowns).")
    public ResponseEntity<List<ProjectResponse>> getProjects() {
        return ResponseEntity.ok(timesheetService.getActiveProjects());
    }

    // ── Admin / Manager: Approval Workflow ───────────────────────────────────

    @GetMapping("/admin/submitted")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Get Submitted Timesheets(MANAGER,ADMIN)", description = "Fetch every timesheet currently pending review (status = SUBMITTED).")
    public ResponseEntity<List<WeeklyTimesheetResponse>> getSubmittedTimesheets() {
        return ResponseEntity.ok(timesheetService.getSubmittedTimesheets());
    }

    @PutMapping("/admin/approve/{timesheetId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Approve Timesheet(ADMIN, MANAGER)", description = "Approve a submitted timesheet. An optional comment can be added.")
    public ResponseEntity<String> approveTimesheet(
            @PathVariable Long timesheetId,
            @Valid @RequestBody ApproveRejectRequest request) {

        return ResponseEntity.ok(timesheetService.approveTimesheet(timesheetId, request));
    }

    @PutMapping("/admin/reject/{timesheetId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Reject Timesheet(MANAGER,ADMIN)", description = "Reject a submitted timesheet with a mandatory comment explaining the reason for rejection.")
    public ResponseEntity<String> rejectTimesheet(
            @PathVariable Long timesheetId,
            @Valid @RequestBody ApproveRejectRequest request) {

        return ResponseEntity.ok(timesheetService.rejectTimesheet(timesheetId, request));
    }
}