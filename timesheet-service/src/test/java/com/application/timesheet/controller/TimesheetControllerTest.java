package com.application.timesheet.controller;

import com.application.timesheet.dto.*;
import com.application.timesheet.service.TimesheetService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TimesheetController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("TimesheetController — Web Layer Tests")
class TimesheetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TimesheetService timesheetService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /timesheet/entries — success")
    void addEntry_Success() throws Exception {
        TimesheetEntryRequest request = new TimesheetEntryRequest();
        request.setProjectId(1L);
        request.setHoursWorked(BigDecimal.valueOf(8.0));
        request.setWorkDate(LocalDate.now());

        TimesheetEntryResponse response = TimesheetEntryResponse.builder()
                .id(1L)
                .hoursWorked(BigDecimal.valueOf(8.0))
                .build();

        when(timesheetService.addEntry(anyLong(), anyString(), any(TimesheetEntryRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/timesheet/entries")
                .header("X-User-Id", 1L)
                .header("X-User-Email", "user@test.com")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("GET /timesheet/weeks/{date} — success")
    void getWeeklyTimesheet_Success() throws Exception {
        WeeklyTimesheetResponse response = WeeklyTimesheetResponse.builder()
                .timesheetId(100L)
                .status("DRAFT")
                .build();

        when(timesheetService.getWeeklyTimesheet(anyLong(), any(LocalDate.class)))
                .thenReturn(response);

        mockMvc.perform(get("/timesheet/weeks/2023-10-10")
                .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timesheetId").value(100L));
    }

    @Test
    @DisplayName("POST /timesheet/weeks/submit — success")
    void submitTimesheet_Success() throws Exception {
        SubmitTimesheetRequest request = new SubmitTimesheetRequest();
        request.setWeekStartDate(LocalDate.now());

        when(timesheetService.submitTimesheet(anyLong(), anyString(), any(SubmitTimesheetRequest.class)))
                .thenReturn("Submitted");

        mockMvc.perform(post("/timesheet/weeks/submit")
                .header("X-User-Id", 1L)
                .header("X-User-Email", "user@test.com")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Submitted"));
    }

    @Test
    @DisplayName("PUT /timesheet/admin/approve/{id} — success")
    void approveTimesheet_Success() throws Exception {
        ApproveRejectRequest request = new ApproveRejectRequest();
        request.setComment("Approved");

        when(timesheetService.approveTimesheet(anyLong(), any(ApproveRejectRequest.class)))
                .thenReturn("Success");

        mockMvc.perform(put("/timesheet/admin/approve/100")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Success"));
    }

    @Test
    @DisplayName("GET /timesheet/projects — success")
    void getProjects_Success() throws Exception {
        ProjectResponse project = ProjectResponse.builder().id(1L).projectName("P1").build();
        when(timesheetService.getActiveProjects()).thenReturn(List.of(project));

        mockMvc.perform(get("/timesheet/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].projectName").value("P1"));
    }

    @Test
    @DisplayName("PUT /timesheet/entries/{entryId} — success")
    void updateEntry_Success() throws Exception {
        UpdateEntryRequest request = new UpdateEntryRequest();
        request.setHoursWorked(BigDecimal.valueOf(4.0));
        request.setTaskSummary("Bug fixing");

        TimesheetEntryResponse response = TimesheetEntryResponse.builder()
                .id(1L)
                .hoursWorked(BigDecimal.valueOf(4.0))
                .build();

        when(timesheetService.updateEntry(anyLong(), anyLong(), any(UpdateEntryRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/timesheet/entries/1")
                .header("X-User-Id", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("DELETE /timesheet/entries/{entryId} — success")
    void deleteEntry_Success() throws Exception {
        mockMvc.perform(delete("/timesheet/entries/1")
                .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(content().string("Timesheet entry ID 1 has been successfully deleted."));
    }

    @Test
    @DisplayName("POST /timesheet/weeks/recall — success")
    void recallTimesheet_Success() throws Exception {
        SubmitTimesheetRequest request = new SubmitTimesheetRequest();
        request.setWeekStartDate(LocalDate.now());

        when(timesheetService.recallTimesheet(anyLong(), any(SubmitTimesheetRequest.class)))
                .thenReturn("Recalled");

        mockMvc.perform(post("/timesheet/weeks/recall")
                .header("X-User-Id", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Recalled"));
    }

    @Test
    @DisplayName("GET /timesheet/history — success")
    void getHistory_Success() throws Exception {
        WeeklyTimesheetResponse history = WeeklyTimesheetResponse.builder().timesheetId(200L).build();
        when(timesheetService.getAllTimesheets(anyLong())).thenReturn(List.of(history));

        mockMvc.perform(get("/timesheet/history")
                .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].timesheetId").value(200L));
    }

    @Test
    @DisplayName("GET /timesheet/admin/submitted — success")
    void getSubmittedTimesheets_Success() throws Exception {
        WeeklyTimesheetResponse submitted = WeeklyTimesheetResponse.builder().timesheetId(300L).build();
        when(timesheetService.getSubmittedTimesheets()).thenReturn(List.of(submitted));

        mockMvc.perform(get("/timesheet/admin/submitted"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].timesheetId").value(300L));
    }

    @Test
    @DisplayName("PUT /timesheet/admin/reject/{timesheetId} — success")
    void rejectTimesheet_Success() throws Exception {
        ApproveRejectRequest request = new ApproveRejectRequest();
        request.setComment("Incomplete");

        when(timesheetService.rejectTimesheet(anyLong(), any(ApproveRejectRequest.class)))
                .thenReturn("Rejected");

        mockMvc.perform(put("/timesheet/admin/reject/100")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Rejected"));
    }
}
