package com.application.admin.service;

import com.application.admin.dto.*;
import com.application.admin.entity.AdminNotification;
import com.application.admin.exception.AdminException;
import com.application.admin.feign.LeaveClient;
import com.application.admin.feign.TimesheetClient;
import com.application.admin.repository.AdminNotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminService — Unit Tests")
class AdminServiceTest {

    @Mock private TimesheetClient timesheetClient;
    @Mock private LeaveClient leaveClient;
    @Mock private AdminNotificationRepository notificationRepository;

    @InjectMocks
    private AdminService adminService;

    @Test
    @DisplayName("getPendingTimesheets() — success")
    void getPendingTimesheets_Success() {
        TimesheetResponseDTO dto = new TimesheetResponseDTO();
        when(timesheetClient.getPendingTimesheets()).thenReturn(List.of(dto));

        var result = adminService.getPendingTimesheets();

        assertThat(result).hasSize(1);
        verify(timesheetClient).getPendingTimesheets();
    }

    @Test
    @DisplayName("approveTimesheet() — success")
    void approveTimesheet_Success() {
        when(timesheetClient.approveTimesheet(eq(1L), any(ApprovalRequestDTO.class)))
                .thenReturn("Approved");

        String result = adminService.approveTimesheet(1L, "Good");

        assertThat(result).isEqualTo("Approved");
    }

    @Test
    @DisplayName("rejectTimesheet() — failure: missing comment")
    void rejectTimesheet_NoComment_ThrowsException() {
        assertThatThrownBy(() -> adminService.rejectTimesheet(1L, ""))
                .isInstanceOf(AdminException.class)
                .hasMessageContaining("mandatory");
    }

    @Test
    @DisplayName("approveLeave() — success")
    void approveLeave_Success() {
        when(leaveClient.approveLeave(eq(100L), any(ApprovalRequestDTO.class)))
                .thenReturn("Approved");

        String result = adminService.approveLeave(100L, 1L, "OK");

        assertThat(result).isEqualTo("Approved");
    }

    @Test
    @DisplayName("rejectTimesheet() — success")
    void rejectTimesheet_Success() {
        when(timesheetClient.rejectTimesheet(eq(1L), any(ApprovalRequestDTO.class)))
                .thenReturn("Rejected");

        String result = adminService.rejectTimesheet(1L, "Not clear enough.");

        assertThat(result).isEqualTo("Rejected");
    }

    @Test
    @DisplayName("getPendingLeaveRequests() — success")
    void getPendingLeaveRequests_Success() {
        LeaveResponseDTO dto = new LeaveResponseDTO();
        when(leaveClient.getPendingLeaveRequests()).thenReturn(List.of(dto));

        var result = adminService.getPendingLeaveRequests();

        assertThat(result).hasSize(1);
        verify(leaveClient).getPendingLeaveRequests();
    }

    @Test
    @DisplayName("rejectLeave() — success")
    void rejectLeave_Success() {
        when(leaveClient.rejectLeave(eq(100L), any(ApprovalRequestDTO.class)))
                .thenReturn("Rejected");

        String result = adminService.rejectLeave(100L, 1L, "Too busy.");

        assertThat(result).isEqualTo("Rejected");
    }

    @Test
    @DisplayName("rejectLeave() — failure: missing comment")
    void rejectLeave_NoComment_ThrowsException() {
        assertThatThrownBy(() -> adminService.rejectLeave(100L, 1L, ""))
                .isInstanceOf(AdminException.class)
                .hasMessageContaining("mandatory");
    }

    @Test
    @DisplayName("getAllNotifications() — success")
    void getAllNotifications_Success() {
        AdminNotification note = new AdminNotification();
        when(notificationRepository.findAll()).thenReturn(List.of(note));

        var result = adminService.getAllNotifications();

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("getNotificationsByUser() — success")
    void getNotificationsByUser_Success() {
        AdminNotification note = new AdminNotification();
        when(notificationRepository.findByUserEmailOrderByReceivedAtDesc("test@test.com"))
                .thenReturn(List.of(note));

        var result = adminService.getNotificationsByUser("test@test.com");

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("getNotificationsByType() — success")
    void getNotificationsByType_Success() {
        AdminNotification note = new AdminNotification();
        when(notificationRepository.findByEventTypeOrderByReceivedAtDesc("USER_REGISTERED"))
                .thenReturn(List.of(note));

        var result = adminService.getNotificationsByType("user_registered");

        assertThat(result).hasSize(1);
    }
}
