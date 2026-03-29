package com.application.timesheet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.timesheet.dto.ApproveRejectRequest;
import com.application.timesheet.dto.ProjectResponse;
import com.application.timesheet.dto.SubmitTimesheetRequest;
import com.application.timesheet.dto.TimesheetEntryRequest;
import com.application.timesheet.dto.TimesheetEntryResponse;
import com.application.timesheet.dto.UpdateEntryRequest;
import com.application.timesheet.dto.WeeklyTimesheetResponse;
import com.application.timesheet.entity.Project;
import com.application.timesheet.entity.Timesheet;
import com.application.timesheet.entity.Timesheet.TimesheetStatus;
import com.application.timesheet.entity.TimesheetEntry;
import com.application.timesheet.exception.TimesheetException;
import com.application.timesheet.messaging.TimesheetEventPublisher;
import com.application.timesheet.repository.ProjectRepository;
import com.application.timesheet.repository.TimesheetEntryRepository;
import com.application.timesheet.repository.TimesheetRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimesheetService {

	private final TimesheetRepository timesheetRepo;
	private final TimesheetEntryRepository entryRepo;
	private final ProjectRepository projectRepo;
	private final TimesheetEventPublisher eventPublisher;

	// Max hours allowed per week
	private static final double MAX_WEEKLY_HOURS = 60.0;
	// Max hours allowed per day (across all projects)
	private static final double MAX_DAILY_HOURS = 12.0;

	// EMPLOYEE OPERATIONS
	@Transactional
	public TimesheetEntryResponse addEntry(Long employeeId, String employeeName, TimesheetEntryRequest request) {

		// Step 1: Validate project
		Project project = projectRepo.findById(request.getProjectId())
				.orElseThrow(() -> new TimesheetException("Project not found with ID: " + request.getProjectId()));

		if (!project.isActive()) {
			throw new TimesheetException("Project is inactive: " + project.getProjectName());
		}

		// Step 2: Reject weekend entries
		DayOfWeek dayOfWeek = request.getWorkDate().getDayOfWeek();
		if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
			throw new TimesheetException("Cannot log work on weekends. '" + request.getWorkDate()
					+ "' is a " + dayOfWeek + ".");
		}

		// Step 3: Reject future-date entries
		if (request.getWorkDate().isAfter(LocalDate.now())) {
			throw new TimesheetException("Cannot log hours for a future date: " + request.getWorkDate());
		}

		// Step 4: Validate daily hours per single entry (DTO max is 12.0 but guard here
		// too)
		if (request.getHoursWorked().doubleValue() > MAX_DAILY_HOURS) {
			throw new TimesheetException("Hours per entry cannot exceed " + MAX_DAILY_HOURS + " hours.");
		}

		// Step 5: Find or create weekly timesheet — always anchored to the Monday of
		// that week
		LocalDate weekStart = request.getWorkDate().with(DayOfWeek.MONDAY);
		Timesheet timesheet = timesheetRepo.findByEmployeeIdAndWeekStartDate(employeeId, weekStart)
				.orElseGet(() -> createNewTimesheet(employeeId, employeeName, weekStart));

		// Step 6: Check timesheet is still editable (DRAFT or REJECTED)
		if (timesheet.getStatus() == TimesheetStatus.SUBMITTED
				|| timesheet.getStatus() == TimesheetStatus.APPROVED) {
			throw new TimesheetException(
					"Cannot add entries to a " + timesheet.getStatus() + " timesheet.");
		}

		// Step 7: Check for duplicate entry (same project on same date)
		if (entryRepo.existsByTimesheetIdAndWorkDateAndProjectId(
				timesheet.getId(), request.getWorkDate(), project.getId())) {
			throw new TimesheetException("Entry already exists for project '"
					+ project.getProjectName() + "' on " + request.getWorkDate()
					+ ". Please update the existing entry.");
		}

		// Step 8: Validate total daily hours across all projects won't exceed limit
		List<TimesheetEntry> dayEntries = entryRepo.findByTimesheetIdAndWorkDate(
				timesheet.getId(), request.getWorkDate());
		double existingDayHours = dayEntries.stream()
				.mapToDouble(e -> e.getHoursWorked().doubleValue()).sum();

		if (existingDayHours + request.getHoursWorked().doubleValue() > MAX_DAILY_HOURS) {
			throw new TimesheetException("Total hours for " + request.getWorkDate()
					+ " would exceed " + MAX_DAILY_HOURS + " hours. "
					+ "Currently logged: " + existingDayHours + " hours.");
		}

		// Step 9: Save entry
		TimesheetEntry entry = TimesheetEntry.builder()
				.timesheet(timesheet)
				.workDate(request.getWorkDate())
				.project(project)
				.hoursWorked(request.getHoursWorked())
				.taskSummary(request.getTaskSummary())
				.build();

		TimesheetEntry saved = entryRepo.save(entry);

		// Step 10: Recalculate and persist total hours on the timesheet
		Double totalHours = entryRepo.sumHoursByTimesheetId(timesheet.getId());
		timesheet.setTotalHours(totalHours != null ? totalHours : 0.0);
		timesheetRepo.save(timesheet);

		log.info("Entry added: employee={}, date={}, project={}, hours={}",
				employeeId, request.getWorkDate(), project.getProjectName(), request.getHoursWorked());

		return mapToEntryResponse(saved);
	}

	@Transactional
	public TimesheetEntryResponse updateEntry(Long entryId, Long employeeId, UpdateEntryRequest request) {

		// Step 1: Find the entry
		TimesheetEntry entry = entryRepo.findById(entryId)
				.orElseThrow(() -> new TimesheetException("Entry not found with ID: " + entryId));

		// Step 2: Verify the entry belongs to the calling employee
		Timesheet timesheet = entry.getTimesheet();
		if (!timesheet.getEmployeeId().equals(employeeId)) {
			throw new TimesheetException("You do not have permission to update this entry.");
		}

		// Step 3: Timesheet must be DRAFT or REJECTED to allow edits
		if (timesheet.getStatus() == TimesheetStatus.SUBMITTED
				|| timesheet.getStatus() == TimesheetStatus.APPROVED) {
			throw new TimesheetException(
					"Cannot update entries in a " + timesheet.getStatus() + " timesheet.");
		}

		// Step 4: Validate new daily total won't exceed limit (exclude current entry)
		List<TimesheetEntry> dayEntries = entryRepo.findByTimesheetIdAndWorkDate(
				timesheet.getId(), entry.getWorkDate());
		double existingDayHours = dayEntries.stream()
				.filter(e -> !e.getId().equals(entryId)) // exclude the entry being updated
				.mapToDouble(e -> e.getHoursWorked().doubleValue())
				.sum();

		if (existingDayHours + request.getHoursWorked().doubleValue() > MAX_DAILY_HOURS) {
			throw new TimesheetException("Updated hours for " + entry.getWorkDate()
					+ " would exceed " + MAX_DAILY_HOURS + " hours. "
					+ "Other entries that day: " + existingDayHours + " hours.");
		}

		// Step 5: Apply updates
		entry.setHoursWorked(request.getHoursWorked());
		entry.setTaskSummary(request.getTaskSummary());
		TimesheetEntry saved = entryRepo.save(entry);

		// Step 6: Recalculate total hours on timesheet
		Double totalHours = entryRepo.sumHoursByTimesheetId(timesheet.getId());
		timesheet.setTotalHours(totalHours != null ? totalHours : 0.0);
		timesheetRepo.save(timesheet);

		log.info("Entry updated: entryId={}, employee={}, newHours={}",
				entryId, employeeId, request.getHoursWorked());

		return mapToEntryResponse(saved);
	}

	@Transactional
	public void deleteEntry(Long entryId, Long employeeId) {

		// Step 1: Find the entry
		TimesheetEntry entry = entryRepo.findById(entryId)
				.orElseThrow(() -> new TimesheetException("Entry not found with ID: " + entryId));

		// Step 2: Verify ownership
		Timesheet timesheet = entry.getTimesheet();
		if (!timesheet.getEmployeeId().equals(employeeId)) {
			throw new TimesheetException("You do not have permission to delete this entry.");
		}

		// Step 3: Timesheet must be DRAFT or REJECTED
		if (timesheet.getStatus() == TimesheetStatus.SUBMITTED
				|| timesheet.getStatus() == TimesheetStatus.APPROVED) {
			throw new TimesheetException(
					"Cannot delete entries from a " + timesheet.getStatus() + " timesheet.");
		}

		// Step 4: Delete and recalculate total hours
		entryRepo.delete(entry);

		Double totalHours = entryRepo.sumHoursByTimesheetId(timesheet.getId());
		timesheet.setTotalHours(totalHours != null ? totalHours : 0.0);
		timesheetRepo.save(timesheet);

		log.info("Entry deleted: entryId={}, employee={}", entryId, employeeId);
	}

	public WeeklyTimesheetResponse getWeeklyTimesheet(Long employeeId, LocalDate weekStart) {
		// Normalise to Monday so any day of the week works as input
		LocalDate monday = weekStart.with(DayOfWeek.MONDAY);

		Timesheet timesheet = timesheetRepo.findByEmployeeIdAndWeekStartDate(employeeId, monday)
				.orElseThrow(() -> new TimesheetException(
						"No timesheet found for week starting: " + monday));

		List<TimesheetEntry> entries = entryRepo.findByTimesheetId(timesheet.getId());
		return mapToWeeklyResponse(timesheet, entries);
	}

	@Transactional
	public String submitTimesheet(Long employeeId, String employeeEmail, SubmitTimesheetRequest request) {

		// Normalise to Monday so any day of the week works as input
		LocalDate monday = request.getWeekStartDate().with(DayOfWeek.MONDAY);

		// Step 1: Find the timesheet
		Timesheet timesheet = timesheetRepo.findByEmployeeIdAndWeekStartDate(employeeId, monday)
				.orElseThrow(() -> new TimesheetException(
						"Timesheet not found for week of: " + monday));

		// Step 2: Only DRAFT and REJECTED can be (re)submitted
		if (timesheet.getStatus() == TimesheetStatus.SUBMITTED) {
			throw new TimesheetException(
					"Timesheet is already SUBMITTED. Use the recall endpoint if you need to make changes.");
		}
		if (timesheet.getStatus() == TimesheetStatus.APPROVED) {
			throw new TimesheetException("Timesheet is already APPROVED and cannot be resubmitted.");
		}

		// Step 3: Must have at least one entry
		List<TimesheetEntry> entries = entryRepo.findByTimesheetId(timesheet.getId());
		if (entries.isEmpty()) {
			throw new TimesheetException(
					"Cannot submit an empty timesheet. Please add work entries first.");
		}

		// Step 4: All Mon–Fri must have at least one entry
		List<DayOfWeek> workDays = List.of(
				DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
				DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);

		List<DayOfWeek> loggedDays = entries.stream()
				.map(e -> e.getWorkDate().getDayOfWeek())
				.distinct()
				.collect(Collectors.toList());

		List<String> missingDays = workDays.stream()
				.filter(day -> !loggedDays.contains(day))
				.map(DayOfWeek::name)
				.collect(Collectors.toList());

		if (!missingDays.isEmpty()) {
			throw new TimesheetException("Missing entries for: "
					+ String.join(", ", missingDays)
					+ ". All working days must have at least one entry.");
		}

		// Step 5: Total weekly hours check
		double totalHours = entries.stream()
				.mapToDouble(e -> e.getHoursWorked().doubleValue()).sum();

		if (totalHours > MAX_WEEKLY_HOURS) {
			throw new TimesheetException("Total hours (" + totalHours
					+ ") exceed the weekly limit of " + MAX_WEEKLY_HOURS + " hours.");
		}

		// Step 6: Update status to SUBMITTED
		timesheet.setStatus(TimesheetStatus.SUBMITTED);
		timesheet.setTotalHours(totalHours);
		timesheet.setSubmittedAt(LocalDateTime.now());
		timesheetRepo.save(timesheet);

		log.info("Timesheet submitted: employee={}, week={}, hours={}", employeeId, monday, totalHours);

		// Step 7: Publish RabbitMQ event
		eventPublisher.publishTimesheetSubmitted(employeeId, employeeEmail, request.getWeekStartDate(), totalHours);

		return "Timesheet submitted successfully for week of " + monday
				+ ". Total hours: " + totalHours;
	}

	@Transactional
	public String recallTimesheet(Long employeeId, SubmitTimesheetRequest request) {

		LocalDate monday = request.getWeekStartDate().with(DayOfWeek.MONDAY);

		Timesheet timesheet = timesheetRepo.findByEmployeeIdAndWeekStartDate(employeeId, monday)
				.orElseThrow(() -> new TimesheetException(
						"Timesheet not found for week of: " + monday));

		// Only SUBMITTED timesheets can be recalled (not APPROVED / REJECTED)
		if (timesheet.getStatus() != TimesheetStatus.SUBMITTED) {
			throw new TimesheetException(
					"Only SUBMITTED timesheets can be recalled. Current status: "
							+ timesheet.getStatus());
		}

		timesheet.setStatus(TimesheetStatus.DRAFT);
		timesheet.setSubmittedAt(null);
		timesheetRepo.save(timesheet);

		log.info("Timesheet recalled: employee={}, week={}", employeeId, monday);

		return "Timesheet for week of " + monday
				+ " has been recalled and is now back in DRAFT status.";
	}

	public List<WeeklyTimesheetResponse> getAllTimesheets(Long employeeId) {
		return timesheetRepo.findByEmployeeIdOrderByWeekStartDateDesc(employeeId).stream()
				.map(ts -> {
					List<TimesheetEntry> entries = entryRepo.findByTimesheetId(ts.getId());
					return mapToWeeklyResponse(ts, entries);
				})
				.collect(Collectors.toList());
	}

	public List<ProjectResponse> getActiveProjects() {
		return projectRepo.findByIsActiveTrue().stream()
				.map(p -> ProjectResponse.builder()
						.id(p.getId())
						.projectCode(p.getProjectCode())
						.projectName(p.getProjectName())
						.build())
				.collect(Collectors.toList());
	}

	// ADMIN / MANAGER OPERATIONS

	public List<WeeklyTimesheetResponse> getSubmittedTimesheets() {
		return timesheetRepo.findByStatus(TimesheetStatus.SUBMITTED).stream()
				.map(ts -> {
					List<TimesheetEntry> entries = entryRepo.findByTimesheetId(ts.getId());
					return mapToWeeklyResponse(ts, entries);
				})
				.collect(Collectors.toList());
	}

	@Transactional
	public String approveTimesheet(Long timesheetId, ApproveRejectRequest request) {

		Timesheet timesheet = timesheetRepo.findById(timesheetId)
				.orElseThrow(() -> new TimesheetException(
						"Timesheet not found with ID: " + timesheetId));

		if (timesheet.getStatus() != TimesheetStatus.SUBMITTED) {
			throw new TimesheetException(
					"Only SUBMITTED timesheets can be approved. Current status: "
							+ timesheet.getStatus());
		}

		timesheet.setStatus(TimesheetStatus.APPROVED);
		timesheet.setManagerComment(request.getComment());
		timesheetRepo.save(timesheet);

		log.info("Timesheet approved: timesheetId={}, employee={}",
				timesheetId, timesheet.getEmployeeId());

		// Notify employee
		eventPublisher.publishTimesheetStatusUpdated(
				timesheet.getEmployeeName(), // is employeeEmail
				null, // fullName not in Entity, will use email as fallback in consumer
				"APPROVED",
				request.getComment(),
				timesheet.getWeekStartDate());

		return "Timesheet ID " + timesheetId + " approved successfully.";
	}

	@Transactional
	public String rejectTimesheet(Long timesheetId, ApproveRejectRequest request) {

		Timesheet timesheet = timesheetRepo.findById(timesheetId)
				.orElseThrow(() -> new TimesheetException(
						"Timesheet not found with ID: " + timesheetId));

		if (timesheet.getStatus() != TimesheetStatus.SUBMITTED) {
			throw new TimesheetException(
					"Only SUBMITTED timesheets can be rejected. Current status: "
							+ timesheet.getStatus());
		}

		// Rejection comment is mandatory so the employee knows what to fix
		if (request.getComment() == null || request.getComment().isBlank()) {
			throw new TimesheetException(
					"A rejection comment is required. Please explain why the timesheet was rejected.");
		}

		timesheet.setStatus(TimesheetStatus.REJECTED);
		timesheet.setManagerComment(request.getComment());
		timesheetRepo.save(timesheet);

		log.info("Timesheet rejected: timesheetId={}, employee={}",
				timesheetId, timesheet.getEmployeeId());

		// Notify employee
		eventPublisher.publishTimesheetStatusUpdated(
				timesheet.getEmployeeName(), // is employeeEmail
				null, // fullName not in Entity, will use email as fallback in consumer
				"REJECTED",
				request.getComment(),
				timesheet.getWeekStartDate());

		return "Timesheet ID " + timesheetId + " has been rejected.";
	}

	// PRIVATE HELPERS

	private Timesheet createNewTimesheet(Long employeeId, String employeeName, LocalDate weekStart) {
		Timesheet ts = Timesheet.builder()
				.employeeId(employeeId)
				.employeeName(employeeName)
				.weekStartDate(weekStart)
				.status(TimesheetStatus.DRAFT)
				.totalHours(0.0)
				.build();
		return timesheetRepo.save(ts);
	}

	private TimesheetEntryResponse mapToEntryResponse(TimesheetEntry e) {
		return TimesheetEntryResponse.builder()
				.id(e.getId())
				.workDate(e.getWorkDate())
				.projectId(e.getProject().getId())
				.projectName(e.getProject().getProjectName())
				.hoursWorked(e.getHoursWorked())
				.taskSummary(e.getTaskSummary())
				.build();
	}

	private WeeklyTimesheetResponse mapToWeeklyResponse(Timesheet ts, List<TimesheetEntry> entries) {
		return WeeklyTimesheetResponse.builder()
				.timesheetId(ts.getId())
				.employeeId(ts.getEmployeeId())
				.employeeName(ts.getEmployeeName())
				.weekStartDate(ts.getWeekStartDate())
				.status(ts.getStatus().name())
				.totalHours(ts.getTotalHours())
				.managerComment(ts.getManagerComment())
				.entries(entries.stream()
						.map(this::mapToEntryResponse)
						.collect(Collectors.toList()))
				.build();
	}
}