package com.application.timesheet.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ApproveRejectRequest {

    @Size(max = 500, message = "Comment cannot exceed 500 characters")
    private String comment;
}
