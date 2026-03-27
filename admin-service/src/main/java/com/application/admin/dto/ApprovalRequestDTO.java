package com.application.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standard request DTO used by the admin-service when calling approve/reject
 * endpoints in timesheet or leave services.
 * Aligned with ApproveRejectRequest/ApproveRejectLeaveRequest.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalRequestDTO {
    private String comment;
}