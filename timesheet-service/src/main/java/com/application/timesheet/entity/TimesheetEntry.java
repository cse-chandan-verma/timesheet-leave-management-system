package com.application.timesheet.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "timesheet_entries", uniqueConstraints = { @UniqueConstraint(columnNames = { "timesheet_id", "work_date",
		"project_id" }, name = "uk_timesheet_date_project") })
public class TimesheetEntry {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "timesheet_id", nullable = false)
	private Timesheet timesheet;
	@Column(name = "work_date", nullable = false)
	private LocalDate workDate;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	@Column(name = "hours_worked", nullable = false, precision = 4, scale = 2)
	private BigDecimal hoursWorked;
	@Column(name = "task_summary", length = 500)
	private String taskSummary;
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@PrePersist
	protected void onCreate() {
		createdAt = LocalDateTime.now();
		updatedAt = LocalDateTime.now();
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = LocalDateTime.now();
	}
}