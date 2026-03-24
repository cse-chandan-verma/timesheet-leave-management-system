package com.application.timesheet.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimesheetEntryResponse {
	private Long id;
	private LocalDate workDate;
	private Long projectId;
	private String projectName;
	private BigDecimal hoursWorked;
	private String taskSummary;
}
