package com.application.timesheet.dto;

import java.time.LocalDate;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmitTimesheetRequest {
	@NotNull(message = "Week start date is required")
	private LocalDate weekStartDate;
}
