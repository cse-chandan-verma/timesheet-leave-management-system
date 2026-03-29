package com.application.leave.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.leave.dto.ApproveRejectLeaveRequest;
import com.application.leave.dto.HolidayRequest;
import com.application.leave.dto.HolidayResponse;
import com.application.leave.dto.LeaveBalanceResponse;
import com.application.leave.dto.LeaveRequestDto;
import com.application.leave.dto.LeaveResponse;
import com.application.leave.dto.LeaveTypeResponse;
import com.application.leave.entity.Holiday;
import com.application.leave.entity.LeaveBalance;
import com.application.leave.entity.LeaveRequest;
import com.application.leave.entity.LeaveRequest.LeaveStatus;
import com.application.leave.entity.LeaveType;
import com.application.leave.exception.LeaveException;
import com.application.leave.messaging.LeaveEventPublisher;
import com.application.leave.repository.HolidayRepository;
import com.application.leave.repository.LeaveBalanceRepository;
import com.application.leave.repository.LeaveRequestRepository;
import com.application.leave.repository.LeaveTypeRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveService {

        private final LeaveRequestRepository leaveRequestRepo;
        private final LeaveBalanceRepository leaveBalanceRepo;
        private final LeaveTypeRepository leaveTypeRepo;
        private final HolidayRepository holidayRepo;
        private final LeaveEventPublisher eventPublisher;

        // EMPLOYEE: APPLY LEAVE

        @Transactional
        public LeaveResponse applyLeave(Long employeeId, String employeeEmail, LeaveRequestDto request) {

                // Step 1: Validate leave type
                LeaveType leaveType = leaveTypeRepo.findById(request.getLeaveTypeId())
                                .orElseThrow(() -> new LeaveException(
                                                "Leave type not found with ID: " + request.getLeaveTypeId()));

                if (!leaveType.isActive()) {
                        throw new LeaveException(
                                        "Leave type '" + leaveType.getTypeName() + "' is currently inactive.");
                }

                // Step 2: Validate dates
                if (request.getFromDate().isAfter(request.getToDate())) {
                        throw new LeaveException("From date cannot be after To date.");
                }

                if (request.getFromDate().isBefore(LocalDate.now())) {
                        throw new LeaveException("Cannot apply leave for past dates.");
                }

                // Guard: leave spanning two calendar years must be split into two requests
                if (request.getFromDate().getYear() != request.getToDate().getYear()) {
                        throw new LeaveException(
                                        "Leave cannot span across two calendar years. "
                                                        + "Please submit two separate requests — one for each year.");
                }

                // Step 3: Calculate working days (excludes weekends + public holidays)
                List<LocalDate> holidays = holidayRepo
                                .findByHolidayDateBetween(request.getFromDate(), request.getToDate())
                                .stream()
                                .map(Holiday::getHolidayDate)
                                .collect(Collectors.toList());

                int totalDays = calculateWorkingDays(
                                request.getFromDate(), request.getToDate(), holidays);

                if (totalDays == 0) {
                        throw new LeaveException(
                                        "Selected date range has no working days "
                                                        + "(all days are weekends or public holidays).");
                }

                // Step 4: Check leave balance
                int year = request.getFromDate().getYear();
                LeaveBalance balance = leaveBalanceRepo
                                .findByEmployeeIdAndLeaveTypeIdAndYear(employeeId, leaveType.getId(), year)
                                .orElseThrow(() -> new LeaveException(
                                                "No leave balance found for '" + leaveType.getTypeName()
                                                                + "' in year " + year + ". Please contact HR."));

                if (totalDays > balance.getRemainingDays()) {
                        throw new LeaveException(
                                        "Insufficient " + leaveType.getTypeName() + " balance. "
                                                        + "Requested: " + totalDays + " days. "
                                                        + "Available: " + balance.getRemainingDays() + " days.");
                }

                // Step 5: Check for overlapping active leave
                // FIX: statuses passed as typed list — not as raw string literals in JPQL
                boolean overlap = leaveRequestRepo.existsOverlappingLeave(
                                employeeId,
                                request.getFromDate(),
                                request.getToDate(),
                                List.of(LeaveStatus.SUBMITTED, LeaveStatus.APPROVED));

                if (overlap) {
                        throw new LeaveException(
                                        "You already have an active leave request overlapping the selected dates. "
                                                        + "Please check your leave history.");
                }

                // Step 6: Save leave request
                LeaveRequest leaveRequest = LeaveRequest.builder()
                                .employeeId(employeeId)
                                .employeeName(employeeEmail) // Assuming employeeName should now be employeeEmail
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
                                employeeId, employeeEmail, leaveType.getTypeName(),
                                request.getFromDate(), request.getToDate(), totalDays, saved.getId());

                return mapToResponse(saved);
        }

        // EMPLOYEE: GET LEAVE BY ID

        public LeaveResponse getLeaveById(Long employeeId, Long leaveRequestId) {
                LeaveRequest request = leaveRequestRepo.findById(leaveRequestId)
                                .orElseThrow(() -> new LeaveException(
                                                "Leave request not found with ID: " + leaveRequestId));

                if (!request.getEmployeeId().equals(employeeId)) {
                        throw new LeaveException("You do not have permission to view this leave request.");
                }

                return mapToResponse(request);
        }

        // EMPLOYEE: GET LEAVE BALANCE

        public List<LeaveBalanceResponse> getLeaveBalance(Long employeeId) {
                int currentYear = LocalDate.now().getYear();
                return leaveBalanceRepo.findByEmployeeIdAndYear(employeeId, currentYear)
                                .stream()
                                .map(this::mapToBalanceResponse)
                                .collect(Collectors.toList());
        }

        // EMPLOYEE: GET LEAVE HISTORY

        public List<LeaveResponse> getLeaveHistory(Long employeeId) {
                return leaveRequestRepo.findByEmployeeIdOrderByAppliedAtDesc(employeeId)
                                .stream()
                                .map(this::mapToResponse)
                                .collect(Collectors.toList());
        }

        // EMPLOYEE: CANCEL LEAVE

        @Transactional
        public String cancelLeave(Long employeeId, Long leaveRequestId) {

                LeaveRequest request = leaveRequestRepo.findById(leaveRequestId)
                                .orElseThrow(() -> new LeaveException(
                                                "Leave request not found with ID: " + leaveRequestId));

                if (!request.getEmployeeId().equals(employeeId)) {
                        throw new LeaveException("You can only cancel your own leave requests.");
                }

                if (request.getStatus() == LeaveStatus.CANCELLED) {
                        throw new LeaveException("This leave request is already cancelled.");
                }

                if (request.getStatus() == LeaveStatus.REJECTED) {
                        throw new LeaveException("Cannot cancel a rejected leave request.");
                }

                if (request.getStatus() == LeaveStatus.APPROVED) {
                        // Allow cancellation only if leave hasn't started yet
                        if (!request.getFromDate().isAfter(LocalDate.now())) {
                                throw new LeaveException(
                                                "Cannot cancel an APPROVED leave that has already started or is today. "
                                                                + "Please contact HR.");
                        }
                        // Restore balance since it was deducted on approval
                        restoreBalance(employeeId, request);
                }

                request.setStatus(LeaveStatus.CANCELLED);
                leaveRequestRepo.save(request);

                log.info("Leave cancelled: requestId={}, employee={}, status was={}",
                                leaveRequestId, employeeId, request.getStatus());

                return "Leave request #" + leaveRequestId + " cancelled successfully.";
        }

        // EMPLOYEE: GET ACTIVE LEAVE TYPES (for dropdown)

        public List<LeaveTypeResponse> getLeaveTypes() {
                return leaveTypeRepo.findByIsActiveTrue().stream()
                                .map(lt -> LeaveTypeResponse.builder()
                                                .id(lt.getId())
                                                .typeCode(lt.getTypeCode())
                                                .typeName(lt.getTypeName())
                                                .maxDays(lt.getMaxDays())
                                                .build())
                                .collect(Collectors.toList());
        }

        // ADMIN / MANAGER: GET ALL SUBMITTED LEAVES

        public List<LeaveResponse> getAllSubmittedLeaves() {
                return leaveRequestRepo.findByStatus(LeaveStatus.SUBMITTED)
                                .stream()
                                .map(this::mapToResponse)
                                .collect(Collectors.toList());
        }

        // ADMIN / MANAGER: APPROVE LEAVE

        @Transactional
        public String approveLeave(Long leaveRequestId, ApproveRejectLeaveRequest req) {

                LeaveRequest request = leaveRequestRepo.findById(leaveRequestId)
                                .orElseThrow(() -> new LeaveException(
                                                "Leave request not found with ID: " + leaveRequestId));

                if (!request.getEmployeeId().equals(req.getEmployeeId())) {
                        throw new LeaveException("Employee ID mismatch: Leave request #" + leaveRequestId + " does not belong to employee ID " + req.getEmployeeId());
                }

                if (request.getStatus() == LeaveStatus.APPROVED || request.getStatus() == LeaveStatus.CANCELLED) {
                        throw new LeaveException(
                                        "Only SUBMITTED or REJECTED leave requests can be approved. Current status: "
                                                        + request.getStatus());
                }

                // Safety net balance check at time of approval
                int year = request.getFromDate().getYear();
                LeaveBalance balance = leaveBalanceRepo
                                .findByEmployeeIdAndLeaveTypeIdAndYear(
                                                request.getEmployeeId(), request.getLeaveType().getId(), year)
                                .orElseThrow(() -> new LeaveException(
                                                "Leave balance not found for employee. Cannot approve."));

                if (request.getTotalDays() > balance.getRemainingDays()) {
                        throw new LeaveException(
                                        "Cannot approve: employee's current balance ("
                                                        + balance.getRemainingDays()
                                                        + " days) is less than the requested "
                                                        + request.getTotalDays() + " days.");
                }

                // Deduct balance — this is the only place usedDays is incremented
                balance.setUsedDays(balance.getUsedDays() + request.getTotalDays());
                leaveBalanceRepo.save(balance);

                // Update leave request status
                request.setStatus(LeaveStatus.APPROVED);
                request.setManagerComment(req.getComment());
                leaveRequestRepo.save(request);

                log.info("Leave approved: requestId={}, employee={}, days={}, balanceRemaining={}",
                                leaveRequestId, request.getEmployeeId(),
                                request.getTotalDays(), balance.getRemainingDays());

                // Notify employee
                eventPublisher.publishLeaveStatusUpdated(
                                request.getEmployeeName(), // is employeeEmail
                                null, // fullName not in Entity, will use email as fallback in consumer
                                "APPROVED",
                                req.getComment(),
                                request.getLeaveType().getTypeName(),
                                request.getFromDate(),
                                request.getToDate());

                return "Leave request #" + leaveRequestId + " approved. "
                                + request.getTotalDays() + " days deducted from balance.";
        }

        // ADMIN / MANAGER: REJECT LEAVE

        @Transactional
        public String rejectLeave(Long leaveRequestId, ApproveRejectLeaveRequest req) {

                LeaveRequest request = leaveRequestRepo.findById(leaveRequestId)
                                .orElseThrow(() -> new LeaveException(
                                                "Leave request not found with ID: " + leaveRequestId));

                if (!request.getEmployeeId().equals(req.getEmployeeId())) {
                        throw new LeaveException("Employee ID mismatch: Leave request #" + leaveRequestId + " does not belong to employee ID " + req.getEmployeeId());
                }

                if (request.getStatus() == LeaveStatus.REJECTED || request.getStatus() == LeaveStatus.CANCELLED) {
                        throw new LeaveException(
                                        "Only SUBMITTED or APPROVED leave requests can be rejected. Current status: "
                                                        + request.getStatus());
                }

                // If rejecting an already approved leave, restore the balance!
                if (request.getStatus() == LeaveStatus.APPROVED) {
                        restoreBalance(request.getEmployeeId(), request);
                        log.info("Balance restored for employee {} due to manager rejecting an already approved leave.", request.getEmployeeId());
                }

                // Rejection comment is mandatory so employee knows what to correct
                if (req.getComment() == null || req.getComment().isBlank()) {
                        throw new LeaveException(
                                        "A rejection comment is required. Please explain why the leave was rejected.");
                }

                request.setStatus(LeaveStatus.REJECTED);
                request.setManagerComment(req.getComment());
                leaveRequestRepo.save(request);

                log.info("Leave rejected: requestId={}, employee={}",
                                leaveRequestId, request.getEmployeeId());

                // Notify employee
                eventPublisher.publishLeaveStatusUpdated(
                                request.getEmployeeName(), // is employeeEmail
                                null, // fullName not in Entity, will use email as fallback in consumer
                                "REJECTED",
                                req.getComment(),
                                request.getLeaveType().getTypeName(),
                                request.getFromDate(),
                                request.getToDate());

                // No balance change — balance was never deducted (leave was still pending)
                return "Leave request #" + leaveRequestId + " rejected.";
        }

        // ADMIN: HOLIDAY MANAGEMENT

        @Transactional
        public HolidayResponse addHoliday(HolidayRequest req) {

                if (holidayRepo.existsByHolidayDate(req.getHolidayDate())) {
                        throw new LeaveException(
                                        "A holiday is already registered for " + req.getHolidayDate() + ".");
                }

                Holiday holiday = Holiday.builder()
                                .holidayDate(req.getHolidayDate())
                                .holidayName(req.getHolidayName())
                                .description(req.getDescription())
                                .build();

                Holiday saved = holidayRepo.save(holiday);
                log.info("Holiday added: {} on {}", req.getHolidayName(), req.getHolidayDate());

                return mapToHolidayResponse(saved);
        }

        public List<HolidayResponse> getHolidaysByYear(int year) {
                LocalDate yearStart = LocalDate.of(year, 1, 1);
                LocalDate yearEnd = LocalDate.of(year, 12, 31);
                return holidayRepo
                                .findByHolidayDateBetweenOrderByHolidayDateAsc(yearStart, yearEnd)
                                .stream()
                                .map(this::mapToHolidayResponse)
                                .collect(Collectors.toList());
        }

        @Transactional
        public String deleteHoliday(Long holidayId) {
                Holiday holiday = holidayRepo.findById(holidayId)
                                .orElseThrow(() -> new LeaveException(
                                                "Holiday not found with ID: " + holidayId));
                holidayRepo.delete(holiday);
                log.info("Holiday deleted: id={}, name={}", holidayId, holiday.getHolidayName());
                return "Holiday deleted successfully.";
        }

        // INITIALIZE LEAVE BALANCE FOR NEW USER

        @Transactional
        public void initializeLeaveBalance(Long employeeId) {
                int currentYear = LocalDate.now().getYear();

                // Check if balance already exists (idempotency)
                boolean exists = leaveBalanceRepo.existsByEmployeeIdAndYear(employeeId, currentYear);
                if (exists) {
                        log.warn("Leave balances already exist for employee {} in year {}. Skipping initialization.",
                                        employeeId, currentYear);
                        return;
                }

                log.info("Initializing leave balances for new employee: {}", employeeId);

                initializeType(employeeId, "CL", "Casual Leave", 15, currentYear);
                initializeType(employeeId, "SL", "Sick Leave", 12, currentYear);
                initializeType(employeeId, "EL", "Earned Leave", 15, currentYear);
                initializeType(employeeId, "COL", "Compensatory Off", 10, currentYear);
        }

        private void initializeType(Long empId, String code, String name, int days, int year) {
                // Find or create LeaveType
                LeaveType type = leaveTypeRepo.findByTypeCode(code)
                                .orElseGet(() -> {
                                        log.info("Leave type {} not found, creating it...", code);
                                        return leaveTypeRepo.save(LeaveType.builder()
                                                        .typeCode(code)
                                                        .typeName(name)
                                                        .maxDays(days)
                                                        .isActive(true)
                                                        .build());
                                });

                // Create Balance
                LeaveBalance balance = LeaveBalance.builder()
                                .employeeId(empId)
                                .leaveType(type)
                                .year(year)
                                .totalDays(days)
                                .usedDays(0)
                                .build();

                leaveBalanceRepo.save(balance);
                log.debug("Created {} balance for employee {}: {} days", code, empId, days);
        }

        // PRIVATE HELPERS

        private int calculateWorkingDays(LocalDate from, LocalDate to,
                        List<LocalDate> holidays) {
                Set<LocalDate> holidaySet = Set.copyOf(holidays);
                int workingDays = 0;
                LocalDate current = from;

                while (!current.isAfter(to)) {
                        DayOfWeek day = current.getDayOfWeek();
                        if (day != DayOfWeek.SATURDAY
                                        && day != DayOfWeek.SUNDAY
                                        && !holidaySet.contains(current)) {
                                workingDays++;
                        }
                        current = current.plusDays(1);
                }
                return workingDays;
        }

        /**
         * Restores leave balance when an APPROVED leave is cancelled before it starts.
         */
        private void restoreBalance(Long employeeId, LeaveRequest request) {
                int year = request.getFromDate().getYear();
                leaveBalanceRepo
                                .findByEmployeeIdAndLeaveTypeIdAndYear(
                                                employeeId, request.getLeaveType().getId(), year)
                                .ifPresent(balance -> {
                                        int restored = Math.max(0, balance.getUsedDays() - request.getTotalDays());
                                        balance.setUsedDays(restored);
                                        leaveBalanceRepo.save(balance);
                                        log.info("Balance restored: employee={}, days={}, newUsed={}",
                                                        employeeId, request.getTotalDays(), restored);
                                });
        }

        private LeaveResponse mapToResponse(LeaveRequest r) {
                return LeaveResponse.builder()
                                .id(r.getId())
                                .employeeId(r.getEmployeeId())
                                .employeeName(r.getEmployeeName())
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

        private LeaveBalanceResponse mapToBalanceResponse(LeaveBalance b) {
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

        private HolidayResponse mapToHolidayResponse(Holiday h) {
                return HolidayResponse.builder()
                                .id(h.getId())
                                .holidayDate(h.getHolidayDate())
                                .holidayName(h.getHolidayName())
                                .description(h.getDescription())
                                .build();
        }
}