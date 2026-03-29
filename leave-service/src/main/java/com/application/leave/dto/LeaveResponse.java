package com.application.leave.dto;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * SERVER RETURNS THIS after leave is applied or fetched.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveResponse {
    private Long          id;
    private Long          employeeId;
    private String        employeeName;
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