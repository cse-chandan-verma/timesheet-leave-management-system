
package com.application.leave.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.leave.dto.LeaveBalanceResponse;
import com.application.leave.dto.LeaveRequestDto;
import com.application.leave.dto.LeaveResponse;
import com.application.leave.entity.LeaveBalance;
import com.application.leave.entity.LeaveRequest;
import com.application.leave.entity.LeaveRequest.LeaveStatus;
import com.application.leave.entity.LeaveType;
import com.application.leave.messaging.LeaveEventPublisher;
import com.application.leave.repository.LeaveBalanceRepository;
import com.application.leave.repository.LeaveRequestRepository;
import com.application.leave.repository.LeaveTypeRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveService {

    private final LeaveRequestRepository  leaveRequestRepo;
    private final LeaveBalanceRepository  leaveBalanceRepo;
    private final LeaveTypeRepository     leaveTypeRepo;
    private final LeaveEventPublisher     eventPublisher;


    // ══════════════════════════════════════════════════════════
    // 1. APPLY LEAVE
    // ══════════════════════════════════════════════════════════

    /**
     * FLOW:
     *   1. Validate leave type exists
     *   2. Validate dates (from <= to, not in past)
     *   3. Calculate working days
     *   4. Check leave balance is sufficient
     *   5. Check no overlapping leave
     *   6. Save request
     *   7. Publish RabbitMQ event
     */
    @Transactional
    public LeaveResponse applyLeave(Long employeeId,
                                     String employeeName,
                                     LeaveRequestDto request) {

        // Step 1: Validate leave type
        LeaveType leaveType = leaveTypeRepo
                .findById(request.getLeaveTypeId())
                .orElseThrow(() -> new RuntimeException(
                        "Leave type not found with ID: "
                        + request.getLeaveTypeId()));

        if (!leaveType.isActive()) {
            throw new RuntimeException(
                    "Leave type '" + leaveType.getTypeName()
                    + "' is currently inactive");
        }

        // Step 2: Validate dates
        if (request.getFromDate().isAfter(request.getToDate())) {
            throw new RuntimeException(
                    "From date cannot be after To date");
        }

        if (request.getFromDate().isBefore(LocalDate.now())) {
            throw new RuntimeException(
                    "Cannot apply leave for past dates");
        }

        // Step 3: Calculate working days (Mon-Fri only)
        int totalDays = calculateWorkingDays(
                request.getFromDate(), request.getToDate());

        if (totalDays == 0) {
            throw new RuntimeException(
                    "Selected date range has no working days");
        }

        // Step 4: Check leave balance
        int year = request.getFromDate().getYear();
        LeaveBalance balance = leaveBalanceRepo
                .findByEmployeeIdAndLeaveTypeIdAndYear(
                        employeeId, leaveType.getId(), year)
                .orElseThrow(() -> new RuntimeException(
                        "No leave balance found for "
                        + leaveType.getTypeName()
                        + " in year " + year
                        + ". Please contact HR."));

        if (totalDays > balance.getRemainingDays()) {
            throw new RuntimeException(
                    "Insufficient " + leaveType.getTypeName()
                    + " balance. Requested: " + totalDays
                    + " days. Available: "
                    + balance.getRemainingDays() + " days.");
        }

        // Step 5: Check overlapping leave
        boolean overlap = leaveRequestRepo.existsOverlappingLeave(
                employeeId,
                request.getFromDate(),
                request.getToDate());

        if (overlap) {
            throw new RuntimeException(
                    "You already have a leave request for the "
                    + "selected date range. "
                    + "Please check your leave history.");
        }

        // Step 6: Save leave request
        LeaveRequest leaveRequest = LeaveRequest.builder()
                .employeeId(employeeId)
                .employeeName(employeeName)
                .leaveType(leaveType)
                .fromDate(request.getFromDate())
                .toDate(request.getToDate())
                .totalDays(totalDays)
                .reason(request.getReason())
                .status(LeaveStatus.SUBMITTED)
                .build();

        LeaveRequest saved = leaveRequestRepo.save(leaveRequest);

        log.info("Leave applied: employee={}, type={}, days={}, from={} to={}",
                employeeId, leaveType.getTypeCode(),
                totalDays, request.getFromDate(), request.getToDate());

        // Step 7: Publish RabbitMQ event
        eventPublisher.publishLeaveApplied(
                employeeId,
                employeeName,
                leaveType.getTypeCode(),
                request.getFromDate(),
                request.getToDate(),
                totalDays,
                saved.getId()
        );

        return mapToResponse(saved);
    }


    // ══════════════════════════════════════════════════════════
    // 2. GET LEAVE BALANCE
    // ══════════════════════════════════════════════════════════

    public List<LeaveBalanceResponse> getLeaveBalance(
            Long employeeId) {

        int currentYear = LocalDate.now().getYear();

        return leaveBalanceRepo
                .findByEmployeeIdAndYear(employeeId, currentYear)
                .stream()
                .map(this::mapToBalanceResponse)
                .collect(Collectors.toList());
    }


    // ══════════════════════════════════════════════════════════
    // 3. GET LEAVE HISTORY
    // ══════════════════════════════════════════════════════════

    public List<LeaveResponse> getLeaveHistory(Long employeeId) {
        return leaveRequestRepo
                .findByEmployeeIdOrderByAppliedAtDesc(employeeId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }


    // ══════════════════════════════════════════════════════════
    // 4. CANCEL LEAVE
    // ══════════════════════════════════════════════════════════

    @Transactional
    public String cancelLeave(Long employeeId, Long leaveRequestId) {

        LeaveRequest request = leaveRequestRepo
                .findById(leaveRequestId)
                .orElseThrow(() -> new RuntimeException(
                        "Leave request not found with ID: "
                        + leaveRequestId));

        // Verify this leave belongs to this employee
        if (!request.getEmployeeId().equals(employeeId)) {
            throw new RuntimeException(
                    "You can only cancel your own leave requests");
        }

        // Only SUBMITTED requests can be cancelled
        if (request.getStatus() == LeaveStatus.APPROVED) {
            throw new RuntimeException(
                    "Cannot cancel an already APPROVED leave. "
                    + "Please contact your manager.");
        }

        if (request.getStatus() == LeaveStatus.CANCELLED) {
            throw new RuntimeException(
                    "This leave request is already cancelled");
        }

        if (request.getStatus() == LeaveStatus.REJECTED) {
            throw new RuntimeException(
                    "Cannot cancel a rejected leave request");
        }

        request.setStatus(LeaveStatus.CANCELLED);
        leaveRequestRepo.save(request);

        log.info("Leave cancelled: requestId={}, employeeId={}",
                leaveRequestId, employeeId);

        return "Leave request #" + leaveRequestId
                + " cancelled successfully";
    }


    // ══════════════════════════════════════════════════════════
    // 5. GET ALL LEAVE TYPES
    // ══════════════════════════════════════════════════════════

    public List<LeaveType> getLeaveTypes() {
        return leaveTypeRepo.findByIsActiveTrue();
    }


    // ══════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════

    /**
     * Calculates working days between two dates (Mon-Fri only).
     * Excludes weekends — does NOT exclude holidays
     * (holiday handling would require a holiday calendar).
     */
    private int calculateWorkingDays(LocalDate from, LocalDate to) {
        int workingDays = 0;
        LocalDate current = from;

        while (!current.isAfter(to)) {
            DayOfWeek day = current.getDayOfWeek();
            if (day != DayOfWeek.SATURDAY
                    && day != DayOfWeek.SUNDAY) {
                workingDays++;
            }
            current = current.plusDays(1);
        }
        return workingDays;
    }

    private LeaveResponse mapToResponse(LeaveRequest r) {
        return LeaveResponse.builder()
                .id(r.getId())
                .leaveTypeName(r.getLeaveType().getTypeName())
                .leaveTypeCode(r.getLeaveType().getTypeCode())
                .fromDate(r.getFromDate())
                .toDate(r.getToDate())
                .totalDays(r.getTotalDays())
                .reason(r.getReason())
                .status(r.getStatus().name())
                .managerComment(r.getManagerComment())
                .appliedAt(r.getAppliedAt())
                .build();
    }

    private LeaveBalanceResponse mapToBalanceResponse(
            LeaveBalance b) {
        return LeaveBalanceResponse.builder()
                .leaveTypeId(b.getLeaveType().getId())
                .leaveTypeCode(b.getLeaveType().getTypeCode())
                .leaveTypeName(b.getLeaveType().getTypeName())
                .totalDays(b.getTotalDays())
                .usedDays(b.getUsedDays())
                .remainingDays(b.getRemainingDays())
                .year(b.getYear())
                .build();
    }
}