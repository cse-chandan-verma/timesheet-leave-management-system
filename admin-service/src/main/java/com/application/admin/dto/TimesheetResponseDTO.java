package com.application.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

/**
 * DTO for timesheet response from timesheet-service.
 * Aligned with WeeklyTimesheetResponse.java in timesheet-service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimesheetResponseDTO {
    private Long      timesheetId;
    private Long      employeeId;
    private String    employeeName;
    private LocalDate weekStartDate;
    private String    status;
    private Double    totalHours;
    private String    managerComment;
}
