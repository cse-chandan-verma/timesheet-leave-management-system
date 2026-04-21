package com.application.admin.controller;

import com.application.admin.dto.*;
import com.application.admin.entity.AdminNotification;
import com.application.admin.service.AdminService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminController — Web Layer Tests")
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminService adminService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /admin/timesheets/pending — success")
    void getPendingTimesheets_Success() throws Exception {
        TimesheetResponseDTO dto = new TimesheetResponseDTO();
        when(adminService.getPendingTimesheets()).thenReturn(List.of(dto));

        mockMvc.perform(get("/admin/timesheets/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Pending timesheets fetched."));
    }

    @Test
    @DisplayName("PUT /admin/timesheets/{id}/approve — success")
    void approveTimesheet_Success() throws Exception {
        ApprovalRequestDTO request = new ApprovalRequestDTO("Good job");
        when(adminService.approveTimesheet(eq(1L), anyString())).thenReturn("Approved successfully");

        mockMvc.perform(put("/admin/timesheets/1/approve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Approved successfully"));
    }

    @Test
    @DisplayName("PUT /admin/timesheets/{id}/reject — success")
    void rejectTimesheet_Success() throws Exception {
        ApprovalRequestDTO request = new ApprovalRequestDTO("Missing details");
        when(adminService.rejectTimesheet(eq(1L), anyString())).thenReturn("Rejected successfully");

        mockMvc.perform(put("/admin/timesheets/1/reject")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Rejected successfully"));
    }

    @Test
    @DisplayName("GET /admin/leaves/pending — success")
    void getPendingLeaves_Success() throws Exception {
        LeaveResponseDTO dto = new LeaveResponseDTO();
        when(adminService.getPendingLeaveRequests()).thenReturn(List.of(dto));

        mockMvc.perform(get("/admin/leaves/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Pending leave requests fetched."));
    }

    @Test
    @DisplayName("PUT /admin/leaves/{id}/approve — success")
    void approveLeave_Success() throws Exception {
        ApprovalRequestDTO request = new ApprovalRequestDTO("Enjoy");
        request.setEmployeeId(10L);
        when(adminService.approveLeave(eq(1L), eq(10L), anyString())).thenReturn("Leave approved");

        mockMvc.perform(put("/admin/leaves/1/approve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Leave approved"));
    }

    @Test
    @DisplayName("PUT /admin/leaves/{id}/reject — success")
    void rejectLeave_Success() throws Exception {
        ApprovalRequestDTO request = new ApprovalRequestDTO("Too many absent");
        request.setEmployeeId(10L);
        when(adminService.rejectLeave(eq(1L), eq(10L), anyString())).thenReturn("Leave rejected");

        mockMvc.perform(put("/admin/leaves/1/reject")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Leave rejected"));
    }

    @Test
    @DisplayName("GET /admin/notifications — success")
    void getAllNotifications_Success() throws Exception {
        AdminNotification note = new AdminNotification();
        note.setEventType("USER_REGISTERED");
        when(adminService.getAllNotifications()).thenReturn(List.of(note));

        mockMvc.perform(get("/admin/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].eventType").value("USER_REGISTERED"));
    }

    @Test
    @DisplayName("GET /admin/notifications/type/{type} — success")
    void getNotificationsByType_Success() throws Exception {
        AdminNotification note = new AdminNotification();
        note.setEventType("LEAVE");
        when(adminService.getNotificationsByType("LEAVE")).thenReturn(List.of(note));

        mockMvc.perform(get("/admin/notifications/type/LEAVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].eventType").value("LEAVE"));
    }

    @Test
    @DisplayName("GET /admin/notifications/user/{email} — success")
    void getNotificationsByUser_Success() throws Exception {
        AdminNotification note = new AdminNotification();
        note.setUserEmail("test@test.com");
        when(adminService.getNotificationsByUser("test@test.com")).thenReturn(List.of(note));

        mockMvc.perform(get("/admin/notifications/user/test@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].userEmail").value("test@test.com"));
    }
}
