package com.application.timesheet.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TimesheetEntryRequest {

	@NotNull(message = "Work date is required")
	private LocalDate workDate;

	@NotNull(message = "Project ID is required")
	private Long projectId;

	@NotNull(message = "Hours worked is required")
	@DecimalMin(value = "0.5", message = "Minimum 0.5 hours per entry")
	@DecimalMax(value = "24.0", message = "Cannot exceed 24 hours per day")
	private BigDecimal hoursWorked;

	@Size(max = 500, message = "Task summary cannot exceed 500 characters")
	private String taskSummary;
}