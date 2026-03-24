package com.application.timesheet.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyTimesheetResponse {
	private Long timesheetId;
	private LocalDate weekStartDate;
	private String status;
	private Double totalHours;
	private String managerComment;
	private List<TimesheetEntryResponse> entries;
}