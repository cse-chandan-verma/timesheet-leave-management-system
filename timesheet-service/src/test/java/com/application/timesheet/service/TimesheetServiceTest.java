package com.application.timesheet.service;

import com.application.timesheet.dto.*;
import com.application.timesheet.entity.*;
import com.application.timesheet.entity.Timesheet.TimesheetStatus;
import com.application.timesheet.exception.TimesheetException;
import com.application.timesheet.messaging.TimesheetEventPublisher;
import com.application.timesheet.repository.*;
import com.application.timesheet.feign.AuthServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class TimesheetServiceTest {

    @Mock
    private TimesheetRepository timesheetRepo;

    @Mock
    private TimesheetEntryRepository entryRepo;

    @Mock
    private ProjectRepository projectRepo;

    @Mock
    private TimesheetEventPublisher eventPublisher;

    @Mock
    private AuthServiceClient authServiceClient;

    @InjectMocks
    private TimesheetService service;

    private Project project;
    private Timesheet timesheet;
    private TimesheetEntry entry;

    @BeforeEach
    void setup() {
        project = new Project();
        project.setId(1L);
        project.setProjectName("Test Project");
        project.setActive(true);

        timesheet = Timesheet.builder()
                .id(1L)
                .employeeId(101L)
                .employeeName("Chandan")
                .weekStartDate(LocalDate.now().with(DayOfWeek.MONDAY))
                .status(TimesheetStatus.DRAFT)
                .totalHours(0.0)
                .build();

        entry = TimesheetEntry.builder()
                .id(1L)
                .timesheet(timesheet)
                .project(project)
                .workDate(LocalDate.now().with(DayOfWeek.MONDAY))
                .hoursWorked(BigDecimal.valueOf(5.0))
                .taskSummary("Work")
                .build();
    }

    // Add Entry

    @Test
    void addEntry_success() {
        TimesheetEntryRequest request = new TimesheetEntryRequest();
        request.setProjectId(1L);
        request.setWorkDate(LocalDate.now().with(DayOfWeek.MONDAY));
        request.setHoursWorked(BigDecimal.valueOf(5.0));
        request.setTaskSummary("Task");

        when(projectRepo.findById(1L)).thenReturn(Optional.of(project));
        when(timesheetRepo.findByEmployeeIdAndWeekStartDate(any(), any()))
                .thenReturn(Optional.of(timesheet));
        when(entryRepo.existsByTimesheetIdAndWorkDateAndProjectId(any(), any(), any()))
                .thenReturn(false);
        when(entryRepo.findByTimesheetIdAndWorkDate(any(), any()))
                .thenReturn(Collections.emptyList());
        when(entryRepo.save(any())).thenReturn(entry);
        when(entryRepo.sumHoursByTimesheetId(any())).thenReturn(5.0);

        assertNotNull(service.addEntry(101L, "Chandan", request));
    }

    @Test
    void addEntry_projectNotFound() {
        when(projectRepo.findById(any())).thenReturn(Optional.empty());

        TimesheetEntryRequest request = new TimesheetEntryRequest();
        request.setProjectId(1L);
        request.setWorkDate(LocalDate.now());

        assertThrows(TimesheetException.class,
                () -> service.addEntry(101L, "Chandan", request));
    }

    @Test
    void addEntry_weekend() {
        TimesheetEntryRequest request = new TimesheetEntryRequest();
        request.setProjectId(1L);
        request.setWorkDate(LocalDate.of(2026, 4, 5)); // Sunday

        when(projectRepo.findById(1L)).thenReturn(Optional.of(project));

        assertThrows(TimesheetException.class,
                () -> service.addEntry(101L, "Chandan", request));
    }

    @Test
    void addEntry_futureDate() {
        TimesheetEntryRequest request = new TimesheetEntryRequest();
        request.setProjectId(1L);
        request.setWorkDate(LocalDate.now().plusDays(2));

        when(projectRepo.findById(1L)).thenReturn(Optional.of(project));

        assertThrows(TimesheetException.class,
                () -> service.addEntry(101L, "Chandan", request));
    }

    // Update Entry

    @Test
    void updateEntry_success() {
        UpdateEntryRequest request = new UpdateEntryRequest();
        request.setHoursWorked(BigDecimal.valueOf(5.0));
        request.setTaskSummary("Updated");

        when(entryRepo.findById(1L)).thenReturn(Optional.of(entry));
        when(entryRepo.findByTimesheetIdAndWorkDate(any(), any()))
                .thenReturn(Collections.singletonList(entry));
        when(entryRepo.save(any())).thenReturn(entry);
        when(entryRepo.sumHoursByTimesheetId(any())).thenReturn(6.0);

        assertNotNull(service.updateEntry(1L, 101L, request));
    }

    @Test
    void updateEntry_unauthorized() {
        when(entryRepo.findById(1L)).thenReturn(Optional.of(entry));

        assertThrows(TimesheetException.class,
                () -> service.updateEntry(1L, 999L, new UpdateEntryRequest()));
    }

    // Delete Entry

    @Test
    void deleteEntry_success() {
        when(entryRepo.findById(1L)).thenReturn(Optional.of(entry));
        when(entryRepo.sumHoursByTimesheetId(any())).thenReturn(0.0);

        service.deleteEntry(1L, 101L);

        verify(entryRepo).delete(entry);
    }

    // Submit Timesheet

    @Test
    void submitTimesheet_success() {
        SubmitTimesheetRequest request = new SubmitTimesheetRequest();
        request.setWeekStartDate(timesheet.getWeekStartDate());

        when(timesheetRepo.findByEmployeeIdAndWeekStartDate(any(), any()))
                .thenReturn(Optional.of(timesheet));

        // Mock entries for all Mon-Fri to pass validation
        LocalDate start = timesheet.getWeekStartDate();
        List<TimesheetEntry> entries = List.of(
            TimesheetEntry.builder().workDate(start).hoursWorked(BigDecimal.valueOf(8.0)).project(project).build(),
            TimesheetEntry.builder().workDate(start.plusDays(1)).hoursWorked(BigDecimal.valueOf(8.0)).project(project).build(),
            TimesheetEntry.builder().workDate(start.plusDays(2)).hoursWorked(BigDecimal.valueOf(8.0)).project(project).build(),
            TimesheetEntry.builder().workDate(start.plusDays(3)).hoursWorked(BigDecimal.valueOf(8.0)).project(project).build(),
            TimesheetEntry.builder().workDate(start.plusDays(4)).hoursWorked(BigDecimal.valueOf(8.0)).project(project).build()
        );
        when(entryRepo.findByTimesheetId(any())).thenReturn(entries);

        assertNotNull(service.submitTimesheet(101L, "email@test.com", request));
    }

    @Test
    void submitTimesheet_emptyEntries() {
        SubmitTimesheetRequest request = new SubmitTimesheetRequest();
        request.setWeekStartDate(LocalDate.now());

        when(timesheetRepo.findByEmployeeIdAndWeekStartDate(any(), any()))
                .thenReturn(Optional.of(timesheet));
        when(entryRepo.findByTimesheetId(any())).thenReturn(Collections.emptyList());

        assertThrows(TimesheetException.class,
                () -> service.submitTimesheet(101L, "email@test.com", request));
    }

    // Approve

    @Test
    void approveTimesheet_success() {
        timesheet.setStatus(TimesheetStatus.SUBMITTED);

        when(timesheetRepo.findById(1L)).thenReturn(Optional.of(timesheet));

        ApproveRejectRequest req = new ApproveRejectRequest();
        req.setComment("Good");

        assertNotNull(service.approveTimesheet(1L, req));
    }

    // Reject

    @Test
    void rejectTimesheet_noComment() {
        timesheet.setStatus(TimesheetStatus.SUBMITTED);

        when(timesheetRepo.findById(1L)).thenReturn(Optional.of(timesheet));

        ApproveRejectRequest req = new ApproveRejectRequest();

        assertThrows(TimesheetException.class,
                () -> service.rejectTimesheet(1L, req));
    }

    // Recall

    @Test
    void recallTimesheet_success() {
        timesheet.setStatus(TimesheetStatus.SUBMITTED);
        SubmitTimesheetRequest request = new SubmitTimesheetRequest();
        request.setWeekStartDate(timesheet.getWeekStartDate());

        when(timesheetRepo.findByEmployeeIdAndWeekStartDate(any(), any()))
                .thenReturn(Optional.of(timesheet));

        String result = service.recallTimesheet(101L, request);

        assertEquals(TimesheetStatus.DRAFT, timesheet.getStatus());
        assertTrue(result.contains("recalled"));
    }

    @Test
    void recallTimesheet_invalidStatus() {
        timesheet.setStatus(TimesheetStatus.APPROVED);
        SubmitTimesheetRequest request = new SubmitTimesheetRequest();
        request.setWeekStartDate(timesheet.getWeekStartDate());

        when(timesheetRepo.findByEmployeeIdAndWeekStartDate(any(), any()))
                .thenReturn(Optional.of(timesheet));

        assertThrows(TimesheetException.class,
                () -> service.recallTimesheet(101L, request));
    }

    // Project Management

    @Test
    void createProject_duplicateCode() {
        CreateProjectRequest request = new CreateProjectRequest();
        request.setProjectCode("PROJ1");

        when(projectRepo.existsByProjectCode("PROJ1")).thenReturn(true);

        assertThrows(TimesheetException.class,
                () -> service.createProject(request));
    }

    @Test
    void deleteProject_softDelete() {
        when(projectRepo.findById(1L)).thenReturn(Optional.of(project));
        when(entryRepo.existsByProjectId(1L)).thenReturn(true);

        String result = service.deleteProject(1L);

        assertFalse(project.isActive());
        assertTrue(result.contains("deactivated"));
        verify(projectRepo, never()).delete(any());
    }

    @Test
    void deleteProject_hardDelete() {
        when(projectRepo.findById(1L)).thenReturn(Optional.of(project));
        when(entryRepo.existsByProjectId(1L)).thenReturn(false);

        String result = service.deleteProject(1L);

        assertTrue(result.contains("deleted successfully"));
        verify(projectRepo).delete(project);
    }

    // Manager Views

    @Test
    void getSubmittedTimesheets_emptyTeam() {
        when(authServiceClient.getTeamEmployeeIds(1L)).thenReturn(Collections.emptyList());

        List<WeeklyTimesheetResponse> result = service.getSubmittedTimesheets(1L);

        assertTrue(result.isEmpty());
    }
}