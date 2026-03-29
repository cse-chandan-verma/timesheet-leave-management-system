package com.application.leave.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Used by manager endpoints (approve / reject) to carry an optional comment
 * and the employeeId to verify the targeted request.
 */
@Data
public class ApproveRejectLeaveRequest {

    @NotNull(message = "Employee ID is required to verify the leave request")
    private Long employeeId;

    @Size(max = 500, message = "Comment cannot exceed 500 characters")
    private String comment;
}
