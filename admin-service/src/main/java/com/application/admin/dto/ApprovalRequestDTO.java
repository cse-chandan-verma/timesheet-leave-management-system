package com.application.admin.dto;

// Removed unused import
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standard request DTO used by the admin-service when calling approve/reject
 * endpoints in timesheet or leave services.
 * Aligned with ApproveRejectRequest/ApproveRejectLeaveRequest.
 */
@Data
@NoArgsConstructor
public class ApprovalRequestDTO {
    private Long employeeId;
    private String comment;

    public ApprovalRequestDTO(String comment) {
        this.comment = comment;
    }
}