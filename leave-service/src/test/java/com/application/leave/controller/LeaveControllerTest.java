package com.application.leave.controller;

import com.application.leave.dto.*;
import com.application.leave.service.LeaveService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LeaveController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("LeaveController — Web Layer Tests")
class LeaveControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LeaveService leaveService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /leave/apply — success")
    void applyLeave_Success() throws Exception {
        LeaveRequestDto request = new LeaveRequestDto();
        request.setLeaveTypeId(1L);
        request.setFromDate(LocalDate.now().plusDays(10));
        request.setToDate(LocalDate.now().plusDays(11));
        request.setReason("Family vacation at the beach for 2 days.");

        LeaveResponse response = LeaveResponse.builder()
                .id(100L)
                .status("SUBMITTED")
                .build();

        when(leaveService.applyLeave(anyLong(), anyString(), any(LeaveRequestDto.class)))
                .thenReturn(response);

        mockMvc.perform(post("/leave/apply")
                .header("X-User-Id", 1L)
                .header("X-User-Email", "user@test.com")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100L));
    }

    @Test
    @DisplayName("GET /leave/balance — success")
    void getLeaveBalance_Success() throws Exception {
        LeaveBalanceResponse balance = LeaveBalanceResponse.builder()
                .leaveTypeCode("AL")
                .remainingDays(15)
                .build();

        when(leaveService.getLeaveBalance(anyLong())).thenReturn(List.of(balance));

        mockMvc.perform(get("/leave/balance")
                .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].leaveTypeCode").value("AL"));
    }

    @Test
    @DisplayName("PUT /leave/admin/approve/{id} — success")
    void approveLeave_Success() throws Exception {
        ApproveRejectLeaveRequest request = new ApproveRejectLeaveRequest();
        request.setEmployeeId(1L);
        request.setComment("Approved");

        when(leaveService.approveLeave(anyLong(), any(ApproveRejectLeaveRequest.class)))
                .thenReturn("Approved");

        mockMvc.perform(put("/leave/admin/approve/100")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Approved"));
    }

    @Test
    @DisplayName("GET /leave/types — success")
    void getLeaveTypes_Success() throws Exception {
        LeaveTypeResponse type = LeaveTypeResponse.builder().typeName("Sick").build();
        when(leaveService.getLeaveTypes()).thenReturn(List.of(type));

        mockMvc.perform(get("/leave/types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].typeName").value("Sick"));
    }

    @Test
    @DisplayName("GET /leave/{id} — success")
    void getLeaveById_Success() throws Exception {
        LeaveResponse response = LeaveResponse.builder().id(100L).build();
        when(leaveService.getLeaveById(anyLong(), anyLong())).thenReturn(response);

        mockMvc.perform(get("/leave/100")
                .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100L));
    }

    @Test
    @DisplayName("GET /leave/history — success")
    void getHistory_Success() throws Exception {
        LeaveResponse history = LeaveResponse.builder().id(101L).build();
        when(leaveService.getLeaveHistory(anyLong())).thenReturn(List.of(history));

        mockMvc.perform(get("/leave/history")
                .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(101L));
    }

    @Test
    @DisplayName("PUT /leave/cancel/{id} — success")
    void cancelLeave_Success() throws Exception {
        when(leaveService.cancelLeave(anyLong(), anyLong())).thenReturn("Cancelled");

        mockMvc.perform(put("/leave/cancel/100")
                .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(content().string("Cancelled"));
    }

    @Test
    @DisplayName("GET /leave/admin/pending — success")
    void getPendingLeaves_Success() throws Exception {
        LeaveResponse pending = LeaveResponse.builder().id(102L).build();
        when(leaveService.getAllSubmittedLeaves(anyLong())).thenReturn(List.of(pending));

        mockMvc.perform(get("/leave/admin/pending")
                .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(102L));
    }

    @Test
    @DisplayName("PUT /leave/admin/reject/{id} — success")
    void rejectLeave_Success() throws Exception {
        ApproveRejectLeaveRequest request = new ApproveRejectLeaveRequest();
        request.setEmployeeId(1L);
        request.setComment("No");

        when(leaveService.rejectLeave(anyLong(), any(ApproveRejectLeaveRequest.class)))
                .thenReturn("Rejected");

        mockMvc.perform(put("/leave/admin/reject/100")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Rejected"));
    }

    @Test
    @DisplayName("POST /leave/admin/holidays — success")
    void addHoliday_Success() throws Exception {
        HolidayRequest request = new HolidayRequest();
        request.setHolidayDate(LocalDate.now());
        request.setHolidayName("New Year");

        HolidayResponse response = HolidayResponse.builder().id(1L).holidayName("New Year").build();
        when(leaveService.addHoliday(any(HolidayRequest.class))).thenReturn(response);

        mockMvc.perform(post("/leave/admin/holidays")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("GET /leave/holidays — success")
    void getHolidays_Success() throws Exception {
        HolidayResponse response = HolidayResponse.builder().id(1L).holidayName("New Year").build();
        when(leaveService.getHolidaysByYear(anyInt())).thenReturn(List.of(response));

        mockMvc.perform(get("/leave/holidays").param("year", "2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].holidayName").value("New Year"));
    }

    @Test
    @DisplayName("DELETE /leave/admin/holidays/{id} — success")
    void deleteHoliday_Success() throws Exception {
        when(leaveService.deleteHoliday(anyLong())).thenReturn("Deleted");

        mockMvc.perform(delete("/leave/admin/holidays/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Deleted"));
    }
}
