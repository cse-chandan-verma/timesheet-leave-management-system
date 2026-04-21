package com.application.leave.service;

import com.application.leave.dto.*;
import com.application.leave.entity.*;
import com.application.leave.entity.LeaveRequest.LeaveStatus;
import com.application.leave.exception.LeaveException;
import com.application.leave.messaging.LeaveEventPublisher;
import com.application.leave.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeaveService — Unit Tests")
class LeaveServiceTest {

    @Mock private LeaveRequestRepository leaveRequestRepo;
    @Mock private LeaveBalanceRepository leaveBalanceRepo;
    @Mock private LeaveTypeRepository leaveTypeRepo;
    @Mock private HolidayRepository holidayRepo;
    @Mock private LeaveEventPublisher eventPublisher;

    @InjectMocks
    private LeaveService leaveService;

    // ── Shared fixtures ──────────────────────────────────────────────────────

    private LeaveType activeLeaveType;
    private LeaveBalance leaveBalance;
    private LeaveRequest savedLeaveRequest;

    private static final Long EMP_ID   = 1L;
    private static final String EMP_EMAIL = "emp@example.com";
    private static final Long LEAVE_TYPE_ID = 10L;
    private static final Long LEAVE_REQ_ID  = 100L;

    @BeforeEach
    void setUp() {
        activeLeaveType = LeaveType.builder()
                .id(LEAVE_TYPE_ID)
                .typeCode("CL")
                .typeName("Casual Leave")
                .maxDays(15)
                .isActive(true)
                .build();

        leaveBalance = LeaveBalance.builder()
                .employeeId(EMP_ID)
                .leaveType(activeLeaveType)
                .year(LocalDate.now().getYear())
                .totalDays(15)
                .usedDays(0)
                .build();
        // remainingDays is a derived field — stub it
        leaveBalance = spy(leaveBalance);
        lenient().doReturn(15).when(leaveBalance).getRemainingDays();

        savedLeaveRequest = LeaveRequest.builder()
                .id(LEAVE_REQ_ID)
                .employeeId(EMP_ID)
                .employeeName(EMP_EMAIL)
                .leaveType(activeLeaveType)
                .fromDate(LocalDate.now().plusDays(2))
                .toDate(LocalDate.now().plusDays(4))
                .totalDays(3)
                .reason("Personal work")
                .status(LeaveStatus.SUBMITTED)
                .build();
    }

    // ── Helper to build a LeaveRequestDto ───────────────────────────────────

    private LeaveRequestDto dto(LocalDate from, LocalDate to) {
        LeaveRequestDto dto = new LeaveRequestDto();
        dto.setLeaveTypeId(LEAVE_TYPE_ID);
        dto.setFromDate(from);
        dto.setToDate(to);
        dto.setReason("Test reason");
        return dto;
    }

    // ════════════════════════════════════════════════════════════════════════
    // applyLeave — happy path
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("applyLeave — valid request saves and returns response")
    void applyLeave_happyPath() {
        LocalDate from = LocalDate.now().plusDays(2);
        LocalDate to   = LocalDate.now().plusDays(4); // 3 weekdays (Mon–Fri range assumed)

        when(leaveTypeRepo.findById(LEAVE_TYPE_ID)).thenReturn(Optional.of(activeLeaveType));
        when(holidayRepo.findByHolidayDateBetween(from, to)).thenReturn(List.of());
        when(leaveBalanceRepo.findByEmployeeIdAndLeaveTypeIdAndYear(EMP_ID, LEAVE_TYPE_ID, from.getYear()))
                .thenReturn(Optional.of(leaveBalance));
        when(leaveRequestRepo.existsOverlappingLeave(eq(EMP_ID), eq(from), eq(to), any()))
                .thenReturn(false);
        when(leaveRequestRepo.save(any())).thenReturn(savedLeaveRequest);
        doNothing().when(eventPublisher).publishLeaveApplied(any(), any(), any(), any(), any(), anyInt(), any());

        LeaveResponse response = leaveService.applyLeave(EMP_ID, EMP_EMAIL, dto(from, to));

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(LEAVE_REQ_ID);
        verify(leaveRequestRepo).save(any(LeaveRequest.class));
        verify(eventPublisher).publishLeaveApplied(any(), any(), any(), any(), any(), anyInt(), any());
    }

    // ── applyLeave — validation failures ────────────────────────────────────

    @Test
    @DisplayName("applyLeave — unknown leaveTypeId → LeaveException")
    void applyLeave_unknownLeaveType_throws() {
        when(leaveTypeRepo.findById(LEAVE_TYPE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leaveService.applyLeave(EMP_ID, EMP_EMAIL,
                dto(LocalDate.now().plusDays(1), LocalDate.now().plusDays(2))))
                .isInstanceOf(LeaveException.class)
                .hasMessageContaining("Leave type not found");
    }

    @Test
    @DisplayName("applyLeave — inactive leave type → LeaveException")
    void applyLeave_inactiveLeaveType_throws() {
        activeLeaveType.setActive(false);
        when(leaveTypeRepo.findById(LEAVE_TYPE_ID)).thenReturn(Optional.of(activeLeaveType));

        assertThatThrownBy(() -> leaveService.applyLeave(EMP_ID, EMP_EMAIL,
                dto(LocalDate.now().plusDays(1), LocalDate.now().plusDays(2))))
                .isInstanceOf(LeaveException.class)
                .hasMessageContaining("inactive");
    }

    @Test
    @DisplayName("applyLeave — fromDate after toDate → LeaveException")
    void applyLeave_fromAfterTo_throws() {
        when(leaveTypeRepo.findById(LEAVE_TYPE_ID)).thenReturn(Optional.of(activeLeaveType));

        LocalDate from = LocalDate.now().plusDays(5);
        LocalDate to   = LocalDate.now().plusDays(2);

        assertThatThrownBy(() -> leaveService.applyLeave(EMP_ID, EMP_EMAIL, dto(from, to)))
                .isInstanceOf(LeaveException.class)
                .hasMessageContaining("From date cannot be after To date");
    }

    @Test
    @DisplayName("applyLeave — fromDate in the past → LeaveException")
    void applyLeave_pastDate_throws() {
        when(leaveTypeRepo.findById(LEAVE_TYPE_ID)).thenReturn(Optional.of(activeLeaveType));

        LocalDate from = LocalDate.now().minusDays(3);
        LocalDate to   = LocalDate.now().minusDays(1);

        assertThatThrownBy(() -> leaveService.applyLeave(EMP_ID, EMP_EMAIL, dto(from, to)))
                .isInstanceOf(LeaveException.class)
                .hasMessageContaining("past dates");
    }

    @Test
    @DisplayName("applyLeave — leave spans two calendar years → LeaveException")
    void applyLeave_crossYearLeave_throws() {
        when(leaveTypeRepo.findById(LEAVE_TYPE_ID)).thenReturn(Optional.of(activeLeaveType));

        LocalDate from = LocalDate.of(LocalDate.now().getYear(), 12, 30);
        LocalDate to   = LocalDate.of(LocalDate.now().getYear() + 1, 1, 3);

        assertThatThrownBy(() -> leaveService.applyLeave(EMP_ID, EMP_EMAIL, dto(from, to)))
                .isInstanceOf(LeaveException.class)
                .hasMessageContaining("two calendar years");
    }

    @Test
    @DisplayName("applyLeave — all days are weekends/holidays → LeaveException")
    void applyLeave_noWorkingDays_throws() {
        // Find the next Saturday
    	
    	LocalDate saturday = LocalDate.now().plusDays(1);
    	while (saturday.getDayOfWeek().getValue() != 6) saturday = saturday.plusDays(1);
    	final LocalDate finalSaturday = saturday;   // ✅ effectively final copy
    	LocalDate sunday = saturday.plusDays(1);

    	// then replace saturday with finalSaturday in the when() mock:
    	when(leaveTypeRepo.findById(LEAVE_TYPE_ID)).thenReturn(Optional.of(activeLeaveType));
    	when(holidayRepo.findByHolidayDateBetween(finalSaturday, sunday)).thenReturn(List.of());

    	assertThatThrownBy(() -> leaveService.applyLeave(EMP_ID, EMP_EMAIL, dto(finalSaturday, sunday)))
    	        .isInstanceOf(LeaveException.class)
    	        .hasMessageContaining("no working days");
    }

    @Test
    @DisplayName("applyLeave — no leave balance record → LeaveException")
    void applyLeave_noBalanceRecord_throws() {
        LocalDate from = LocalDate.now().plusDays(2);
        LocalDate to   = LocalDate.now().plusDays(4);

        when(leaveTypeRepo.findById(LEAVE_TYPE_ID)).thenReturn(Optional.of(activeLeaveType));
        when(holidayRepo.findByHolidayDateBetween(from, to)).thenReturn(List.of());
        when(leaveBalanceRepo.findByEmployeeIdAndLeaveTypeIdAndYear(EMP_ID, LEAVE_TYPE_ID, from.getYear()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> leaveService.applyLeave(EMP_ID, EMP_EMAIL, dto(from, to)))
                .isInstanceOf(LeaveException.class)
                .hasMessageContaining("No leave balance found");
    }

    @Test
    @DisplayName("applyLeave — insufficient balance → LeaveException")
    void applyLeave_insufficientBalance_throws() {
        LocalDate from = LocalDate.now().plusDays(2);
        LocalDate to   = LocalDate.now().plusDays(20);

        LeaveBalance tinyBalance = spy(LeaveBalance.builder()
                .employeeId(EMP_ID).leaveType(activeLeaveType)
                .year(from.getYear()).totalDays(1).usedDays(0).build());
        doReturn(1).when(tinyBalance).getRemainingDays();

        when(leaveTypeRepo.findById(LEAVE_TYPE_ID)).thenReturn(Optional.of(activeLeaveType));
        when(holidayRepo.findByHolidayDateBetween(from, to)).thenReturn(List.of());
        when(leaveBalanceRepo.findByEmployeeIdAndLeaveTypeIdAndYear(EMP_ID, LEAVE_TYPE_ID, from.getYear()))
                .thenReturn(Optional.of(tinyBalance));

        assertThatThrownBy(() -> leaveService.applyLeave(EMP_ID, EMP_EMAIL, dto(from, to)))
                .isInstanceOf(LeaveException.class)
                .hasMessageContaining("Insufficient");
    }

    @Test
    @DisplayName("applyLeave — overlapping leave exists → LeaveException")
    void applyLeave_overlappingLeave_throws() {
        LocalDate from = LocalDate.now().plusDays(2);
        LocalDate to   = LocalDate.now().plusDays(4);

        when(leaveTypeRepo.findById(LEAVE_TYPE_ID)).thenReturn(Optional.of(activeLeaveType));
        when(holidayRepo.findByHolidayDateBetween(from, to)).thenReturn(List.of());
        when(leaveBalanceRepo.findByEmployeeIdAndLeaveTypeIdAndYear(EMP_ID, LEAVE_TYPE_ID, from.getYear()))
                .thenReturn(Optional.of(leaveBalance));
        when(leaveRequestRepo.existsOverlappingLeave(eq(EMP_ID), eq(from), eq(to), any()))
                .thenReturn(true);

        assertThatThrownBy(() -> leaveService.applyLeave(EMP_ID, EMP_EMAIL, dto(from, to)))
                .isInstanceOf(LeaveException.class)
                .hasMessageContaining("overlapping");
    }

    // ════════════════════════════════════════════════════════════════════════
    // getLeaveById
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("getLeaveById — found and owned → returns response")
    void getLeaveById_happyPath() {
        when(leaveRequestRepo.findById(LEAVE_REQ_ID)).thenReturn(Optional.of(savedLeaveRequest));

        LeaveResponse resp = leaveService.getLeaveById(EMP_ID, LEAVE_REQ_ID);

        assertThat(resp.getId()).isEqualTo(LEAVE_REQ_ID);
    }

    @Test
    @DisplayName("getLeaveById — not found → LeaveException")
    void getLeaveById_notFound_throws() {
        when(leaveRequestRepo.findById(LEAVE_REQ_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leaveService.getLeaveById(EMP_ID, LEAVE_REQ_ID))
                .isInstanceOf(LeaveException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("getLeaveById — different employee → LeaveException")
    void getLeaveById_wrongEmployee_throws() {
        when(leaveRequestRepo.findById(LEAVE_REQ_ID)).thenReturn(Optional.of(savedLeaveRequest));

        assertThatThrownBy(() -> leaveService.getLeaveById(999L, LEAVE_REQ_ID))
                .isInstanceOf(LeaveException.class)
                .hasMessageContaining("permission");
    }

    // ════════════════════════════════════════════════════════════════════════
    // getLeaveBalance
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("getLeaveBalance — returns mapped balance list")
    void getLeaveBalance_returnsList() {
        when(leaveBalanceRepo.findByEmployeeIdAndYear(eq(EMP_ID), anyInt()))
                .thenReturn(List.of(leaveBalance));

        List<LeaveBalanceResponse> result = leaveService.getLeaveBalance(EMP_ID);

        assertThat(result).hasSize(1);
    }

    
    @Test
    @DisplayName("getLeaveHistory — returns all requests for employee")
    void getLeaveHistory_returnsList() {
        when(leaveRequestRepo.findByEmployeeIdOrderByAppliedAtDesc(EMP_ID))
                .thenReturn(List.of(savedLeaveRequest));

        List<LeaveResponse> result = leaveService.getLeaveHistory(EMP_ID);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("cancelLeave — SUBMITTED leave → cancelled successfully")
    void cancelLeave_submitted_succeeds() {
        when(leaveRequestRepo.findById(LEAVE_REQ_ID)).thenReturn(Optional.of(savedLeaveRequest));
        when(leaveRequestRepo.save(any())).thenReturn(savedLeaveRequest);

        String msg = leaveService.cancelLeave(EMP_ID, LEAVE_REQ_ID);

        assertThat(msg).contains("cancelled successfully");
        assertThat(savedLeaveRequest.getStatus()).isEqualTo(LeaveStatus.CANCELLED);
    }

    @Test
    @DisplayName("cancelLeave — already CANCELLED → LeaveException")
    void cancelLeave_alreadyCancelled_throws() {
        savedLeaveRequest.setStatus(LeaveStatus.CANCELLED);
        when(leaveRequestRepo.findById(LEAVE_REQ_ID)).thenReturn(Optional.of(savedLeaveRequest));

        assertThatThrownBy(() -> leaveService.cancelLeave(EMP_ID, LEAVE_REQ_ID))
                .isInstanceOf(LeaveException.class)
                .hasMessageContaining("already cancelled");
    }

    @Test
    @DisplayName("cancelLeave — REJECTED leave → LeaveException")
    void cancelLeave_rejected_throws() {
        savedLeaveRequest.setStatus(LeaveStatus.REJECTED);
        when(leaveRequestRepo.findById(LEAVE_REQ_ID)).thenReturn(Optional.of(savedLeaveRequest));

        assertThatThrownBy(() -> leaveService.cancelLeave(EMP_ID, LEAVE_REQ_ID))
                .isInstanceOf(LeaveException.class)
                .hasMessageContaining("Cannot cancel a rejected");
    }

    @Test
    @DisplayName("cancelLeave — APPROVED but started today → LeaveException")
    void cancelLeave_approvedStartedToday_throws() {
        savedLeaveRequest.setStatus(LeaveStatus.APPROVED);
        savedLeaveRequest.setFromDate(LocalDate.now()); // started today
        when(leaveRequestRepo.findById(LEAVE_REQ_ID)).thenReturn(Optional.of(savedLeaveRequest));

        assertThatThrownBy(() -> leaveService.cancelLeave(EMP_ID, LEAVE_REQ_ID))
                .isInstanceOf(LeaveException.class)
                .hasMessageContaining("already started");
    }

    @Test
    @DisplayName("cancelLeave — APPROVED and future → restores balance and cancels")
    void cancelLeave_approvedFuture_restoresBalance() {
        savedLeaveRequest.setStatus(LeaveStatus.APPROVED);
        savedLeaveRequest.setFromDate(LocalDate.now().plusDays(5));
        savedLeaveRequest.setTotalDays(3);

        LeaveBalance balance = spy(LeaveBalance.builder()
                .employeeId(EMP_ID).leaveType(activeLeaveType)
                .year(LocalDate.now().getYear()).totalDays(15).usedDays(3).build());

        when(leaveRequestRepo.findById(LEAVE_REQ_ID)).thenReturn(Optional.of(savedLeaveRequest));
        when(leaveBalanceRepo.findByEmployeeIdAndLeaveTypeIdAndYear(EMP_ID, LEAVE_TYPE_ID, LocalDate.now().getYear()))
                .thenReturn(Optional.of(balance));
        when(leaveBalanceRepo.save(any())).thenReturn(balance);
        when(leaveRequestRepo.save(any())).thenReturn(savedLeaveRequest);

        String msg = leaveService.cancelLeave(EMP_ID, LEAVE_REQ_ID);

        assertThat(msg).contains("cancelled successfully");
        assertThat(balance.getUsedDays()).isEqualTo(0); // 3 - 3 = 0
    }

    @Test
    @DisplayName("cancelLeave — wrong employee → LeaveException")
    void cancelLeave_wrongEmployee_throws() {
        when(leaveRequestRepo.findById(LEAVE_REQ_ID)).thenReturn(Optional.of(savedLeaveRequest));

        assertThatThrownBy(() -> leaveService.cancelLeave(999L, LEAVE_REQ_ID))
                .isInstanceOf(LeaveException.class)
                .hasMessageContaining("your own");
    }

    @Test
    @DisplayName("getLeaveTypes — returns active types only")
    void getLeaveTypes_returnsActiveList() {
        when(leaveTypeRepo.findByIsActiveTrue()).thenReturn(List.of(activeLeaveType));

        List<LeaveTypeResponse> result = leaveService.getLeaveTypes();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTypeCode()).isEqualTo("CL");
    }

    @Test
    @DisplayName("getAllSubmittedLeaves — returns SUBMITTED leaves")
    void getAllSubmittedLeaves_returnsList() {
        when(leaveRequestRepo.findByStatus(LeaveStatus.SUBMITTED))
                .thenReturn(List.of(savedLeaveRequest));

        List<LeaveResponse> result = leaveService.getAllSubmittedLeaves();

        assertThat(result).hasSize(1);
    }

    private ApproveRejectLeaveRequest approveReq(String comment) {
        ApproveRejectLeaveRequest r = new ApproveRejectLeaveRequest();
        r.setEmployeeId(EMP_ID);
        r.setComment(comment);
        return r;
    }

    @Test
    @DisplayName("approveLeave — SUBMITTED → approved, balance deducted")
    void approveLeave_happyPath() {
        when(leaveRequestRepo.findById(LEAVE_REQ_ID)).thenReturn(Optional.of(savedLeaveRequest));
        when(leaveBalanceRepo.findByEmployeeIdAndLeaveTypeIdAndYear(EMP_ID, LEAVE_TYPE_ID,
                savedLeaveRequest.getFromDate().getYear()))
                .thenReturn(Optional.of(leaveBalance));
        when(leaveBalanceRepo.save(any())).thenReturn(leaveBalance);
        when(leaveRequestRepo.save(any())).thenReturn(savedLeaveRequest);
        doNothing().when(eventPublisher).publishLeaveStatusUpdated(any(), any(), any(), any(), any(), any(), any());

        String msg = leaveService.approveLeave(LEAVE_REQ_ID, approveReq("Approved"));

        assertThat(msg).contains("approved");
        assertThat(savedLeaveRequest.getStatus()).isEqualTo(LeaveStatus.APPROVED);
    }

    @Test
    @DisplayName("approveLeave — leave not found → LeaveException")
    void approveLeave_notFound_throws() {
        when(leaveRequestRepo.findById(LEAVE_REQ_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leaveService.approveLeave(LEAVE_REQ_ID, approveReq("ok")))
                .isInstanceOf(LeaveException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("approveLeave — employee ID mismatch → LeaveException")
    void approveLeave_employeeIdMismatch_throws() {
        when(leaveRequestRepo.findById(LEAVE_REQ_ID)).thenReturn(Optional.of(savedLeaveRequest));
        ApproveRejectLeaveRequest req = approveReq("ok");
        req.setEmployeeId(999L);

        assertThatThrownBy(() -> leaveService.approveLeave(LEAVE_REQ_ID, req))
                .isInstanceOf(LeaveException.class)
                .hasMessageContaining("mismatch");
    }

    @Test
    @DisplayName("approveLeave — already APPROVED → LeaveException")
    void approveLeave_alreadyApproved_throws() {
        savedLeaveRequest.setStatus(LeaveStatus.APPROVED);
        when(leaveRequestRepo.findById(LEAVE_REQ_ID)).thenReturn(Optional.of(savedLeaveRequest));

        assertThatThrownBy(() -> leaveService.approveLeave(LEAVE_REQ_ID, approveReq("ok")))
                .isInstanceOf(LeaveException.class)
                .hasMessageContaining("SUBMITTED or REJECTED");
    }

    @Test
    @DisplayName("approveLeave — balance not found → LeaveException")
    void approveLeave_noBalance_throws() {
        when(leaveRequestRepo.findById(LEAVE_REQ_ID)).thenReturn(Optional.of(savedLeaveRequest));
        when(leaveBalanceRepo.findByEmployeeIdAndLeaveTypeIdAndYear(any(), any(), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> leaveService.approveLeave(LEAVE_REQ_ID, approveReq("ok")))
                .isInstanceOf(LeaveException.class)
                .hasMessageContaining("balance not found");
    }

    @Test
    @DisplayName("approveLeave — balance insufficient at approval time → LeaveException")
    void approveLeave_insufficientBalance_throws() {
        savedLeaveRequest.setTotalDays(20);
        LeaveBalance tinyBalance = spy(LeaveBalance.builder()
                .employeeId(EMP_ID).leaveType(activeLeaveType)
                .year(LocalDate.now().getYear()).totalDays(1).usedDays(0).build());
        doReturn(1).when(tinyBalance).getRemainingDays();

        when(leaveRequestRepo.findById(LEAVE_REQ_ID)).thenReturn(Optional.of(savedLeaveRequest));
        when(leaveBalanceRepo.findByEmployeeIdAndLeaveTypeIdAndYear(any(), any(), any()))
                .thenReturn(Optional.of(tinyBalance));

        assertThatThrownBy(() -> leaveService.approveLeave(LEAVE_REQ_ID, approveReq("ok")))
                .isInstanceOf(LeaveException.class)
                .hasMessageContaining("Cannot approve");
    }

    @Test
    @DisplayName("rejectLeave — SUBMITTED → rejected successfully")
    void rejectLeave_happyPath() {
        when(leaveRequestRepo.findById(LEAVE_REQ_ID)).thenReturn(Optional.of(savedLeaveRequest));
        when(leaveRequestRepo.save(any())).thenReturn(savedLeaveRequest);
        doNothing().when(eventPublisher).publishLeaveStatusUpdated(any(), any(), any(), any(), any(), any(), any());

        String msg = leaveService.rejectLeave(LEAVE_REQ_ID, approveReq("Not eligible"));

        assertThat(msg).contains("rejected");
        assertThat(savedLeaveRequest.getStatus()).isEqualTo(LeaveStatus.REJECTED);
    }

    @Test
    @DisplayName("rejectLeave — APPROVED leave → restores balance then rejects")
    void rejectLeave_approvedLeave_restoresBalance() {
        savedLeaveRequest.setStatus(LeaveStatus.APPROVED);
        savedLeaveRequest.setTotalDays(3);

        LeaveBalance balance = LeaveBalance.builder()
                .employeeId(EMP_ID).leaveType(activeLeaveType)
                .year(savedLeaveRequest.getFromDate().getYear())
                .totalDays(15).usedDays(3).build();

        when(leaveRequestRepo.findById(LEAVE_REQ_ID)).thenReturn(Optional.of(savedLeaveRequest));
        when(leaveBalanceRepo.findByEmployeeIdAndLeaveTypeIdAndYear(EMP_ID, LEAVE_TYPE_ID,
                savedLeaveRequest.getFromDate().getYear()))
                .thenReturn(Optional.of(balance));
        when(leaveBalanceRepo.save(any())).thenReturn(balance);
        when(leaveRequestRepo.save(any())).thenReturn(savedLeaveRequest);
        doNothing().when(eventPublisher).publishLeaveStatusUpdated(any(), any(), any(), any(), any(), any(), any());

        String msg = leaveService.rejectLeave(LEAVE_REQ_ID, approveReq("Changed"));

        assertThat(msg).contains("rejected");
        assertThat(balance.getUsedDays()).isEqualTo(0);
    }

    @Test
    @DisplayName("rejectLeave — already REJECTED → LeaveException")
    void rejectLeave_alreadyRejected_throws() {
        savedLeaveRequest.setStatus(LeaveStatus.REJECTED);
        when(leaveRequestRepo.findById(LEAVE_REQ_ID)).thenReturn(Optional.of(savedLeaveRequest));

        assertThatThrownBy(() -> leaveService.rejectLeave(LEAVE_REQ_ID, approveReq("reason")))
                .isInstanceOf(LeaveException.class)
                .hasMessageContaining("SUBMITTED or APPROVED");
    }

    @Test
    @DisplayName("rejectLeave — blank comment → LeaveException")
    void rejectLeave_blankComment_throws() {
        when(leaveRequestRepo.findById(LEAVE_REQ_ID)).thenReturn(Optional.of(savedLeaveRequest));

        assertThatThrownBy(() -> leaveService.rejectLeave(LEAVE_REQ_ID, approveReq("")))
                .isInstanceOf(LeaveException.class)
                .hasMessageContaining("rejection comment is required");
    }

    @Test
    @DisplayName("rejectLeave — employee ID mismatch → LeaveException")
    void rejectLeave_employeeIdMismatch_throws() {
        when(leaveRequestRepo.findById(LEAVE_REQ_ID)).thenReturn(Optional.of(savedLeaveRequest));
        ApproveRejectLeaveRequest req = approveReq("reason");
        req.setEmployeeId(999L);

        assertThatThrownBy(() -> leaveService.rejectLeave(LEAVE_REQ_ID, req))
                .isInstanceOf(LeaveException.class)
                .hasMessageContaining("mismatch");
    }

    
    @Test
    @DisplayName("addHoliday — new date → saved and returned")
    void addHoliday_happyPath() {
        HolidayRequest req = new HolidayRequest();
        req.setHolidayDate(LocalDate.of(2025, 8, 15));
        req.setHolidayName("Independence Day");
        req.setDescription("National Holiday");

        Holiday saved = Holiday.builder()
                .id(1L).holidayDate(req.getHolidayDate())
                .holidayName(req.getHolidayName()).description(req.getDescription())
                .build();

        when(holidayRepo.existsByHolidayDate(req.getHolidayDate())).thenReturn(false);
        when(holidayRepo.save(any())).thenReturn(saved);

        HolidayResponse resp = leaveService.addHoliday(req);

        assertThat(resp.getHolidayName()).isEqualTo("Independence Day");
    }

    @Test
    @DisplayName("addHoliday — duplicate date → LeaveException")
    void addHoliday_duplicateDate_throws() {
        HolidayRequest req = new HolidayRequest();
        req.setHolidayDate(LocalDate.of(2025, 8, 15));

        when(holidayRepo.existsByHolidayDate(req.getHolidayDate())).thenReturn(true);

        assertThatThrownBy(() -> leaveService.addHoliday(req))
                .isInstanceOf(LeaveException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    @DisplayName("getHolidaysByYear — returns holidays for given year")
    void getHolidaysByYear_returnsList() {
        Holiday h = Holiday.builder().id(1L)
                .holidayDate(LocalDate.of(2025, 1, 26))
                .holidayName("Republic Day").build();

        when(holidayRepo.findByHolidayDateBetweenOrderByHolidayDateAsc(any(), any()))
                .thenReturn(List.of(h));

        List<HolidayResponse> result = leaveService.getHolidaysByYear(2025);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getHolidayName()).isEqualTo("Republic Day");
    }

    @Test
    @DisplayName("deleteHoliday — found → deleted")
    void deleteHoliday_happyPath() {
        Holiday h = Holiday.builder().id(1L).holidayName("Test").build();
        when(holidayRepo.findById(1L)).thenReturn(Optional.of(h));
        doNothing().when(holidayRepo).delete(h);

        String msg = leaveService.deleteHoliday(1L);

        assertThat(msg).contains("deleted");
        verify(holidayRepo).delete(h);
    }

    @Test
    @DisplayName("deleteHoliday — not found → LeaveException")
    void deleteHoliday_notFound_throws() {
        when(holidayRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leaveService.deleteHoliday(99L))
                .isInstanceOf(LeaveException.class)
                .hasMessageContaining("Holiday not found");
    }

    @Test
    @DisplayName("initializeLeaveBalance — already exists → skips (idempotent)")
    void initializeLeaveBalance_alreadyExists_skips() {
        when(leaveBalanceRepo.existsByEmployeeIdAndYear(EMP_ID, LocalDate.now().getYear()))
                .thenReturn(true);

        leaveService.initializeLeaveBalance(EMP_ID);

        verify(leaveBalanceRepo, never()).save(any());
    }

    @Test
    @DisplayName("initializeLeaveBalance — new employee → creates 4 leave types and balances")
    void initializeLeaveBalance_newEmployee_creates4Balances() {
        when(leaveBalanceRepo.existsByEmployeeIdAndYear(EMP_ID, LocalDate.now().getYear()))
                .thenReturn(false);
        when(leaveTypeRepo.findByTypeCode(any())).thenReturn(Optional.of(activeLeaveType));
        when(leaveBalanceRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        leaveService.initializeLeaveBalance(EMP_ID);

        verify(leaveBalanceRepo, times(4)).save(any(LeaveBalance.class));
    }

    @Test
    @DisplayName("initializeLeaveBalance — leave type missing → auto-creates it")
    void initializeLeaveBalance_missingLeaveType_createsIt() {
        when(leaveBalanceRepo.existsByEmployeeIdAndYear(EMP_ID, LocalDate.now().getYear()))
                .thenReturn(false);
        when(leaveTypeRepo.findByTypeCode(any())).thenReturn(Optional.empty());
        when(leaveTypeRepo.save(any())).thenReturn(activeLeaveType);
        when(leaveBalanceRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        leaveService.initializeLeaveBalance(EMP_ID);

        verify(leaveTypeRepo, times(4)).save(any(LeaveType.class));
    }
}