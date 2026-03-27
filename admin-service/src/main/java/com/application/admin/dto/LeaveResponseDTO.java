package com.application.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for leave response from leave-service.
 * Aligned with LeaveResponse.java in leave-service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveResponseDTO {
    private Long          id;
    private String        leaveTypeName;
    private String        leaveTypeCode;
    private LocalDate     fromDate;
    private LocalDate     toDate;
    private Integer       totalDays;
    private String        reason;
    private String        status;
    private String        managerComment;
    private LocalDateTime appliedAt;
}