package com.application.leave.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Used by manager endpoints (approve / reject) to carry an optional comment.
 * The service enforces that comment is non-blank for rejections.
 */
@Data
public class ApproveRejectLeaveRequest {

    @Size(max = 500, message = "Comment cannot exceed 500 characters")
    private String comment;
}
