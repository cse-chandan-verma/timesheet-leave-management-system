package com.application.timesheet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.timesheet.dto.SubmitTimesheetRequest;
import com.application.timesheet.dto.TimesheetEntryRequest;
import com.application.timesheet.dto.TimesheetEntryResponse;
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
	// Max hours allowed per day
	private static final double MAX_DAILY_HOURS = 12.0;
	@Transactional
	public TimesheetEntryResponse addEntry(Long employeeId, String employeeName, TimesheetEntryRequest request) {

		// Step 1: Validate project
		Project project = projectRepo.findById(request.getProjectId())
				.orElseThrow(() -> new TimesheetException("Project not found with ID: " + request.getProjectId()));

		if (!project.isActive()) {
			throw new TimesheetException("Project is inactive: " + project.getProjectName());
		}

		// Step 2: Validate daily hours
		if (request.getHoursWorked().doubleValue() > MAX_DAILY_HOURS) {
			throw new TimesheetException("Hours per entry cannot exceed " + MAX_DAILY_HOURS);
		}

		// Step 3: Find or create weekly timesheet
		LocalDate weekStart = request.getWorkDate().with(DayOfWeek.MONDAY);

		Timesheet timesheet = timesheetRepo.findByEmployeeIdAndWeekStartDate(employeeId, weekStart)
				.orElseGet(() -> createNewTimesheet(employeeId, employeeName, weekStart));

		// Step 4: Check timesheet is still editable
		if (timesheet.getStatus() == TimesheetStatus.SUBMITTED || timesheet.getStatus() == TimesheetStatus.APPROVED) {
			throw new TimesheetException("Cannot add entries to a " + timesheet.getStatus() + " timesheet");
		}

		// Step 5: Check for duplicate entry
		if (entryRepo.existsByTimesheetIdAndWorkDateAndProjectId(timesheet.getId(), request.getWorkDate(),
				project.getId())) {
			throw new TimesheetException("Entry already exists for project '" + project.getProjectName() + "' on "
					+ request.getWorkDate() + ". Please update the existing entry.");
		}

		// Step 6: Validate total daily hours won't exceed limit
		List<TimesheetEntry> dayEntries = entryRepo.findByTimesheetIdAndWorkDate(timesheet.getId(),
				request.getWorkDate());

		double existingDayHours = dayEntries.stream().mapToDouble(e -> e.getHoursWorked().doubleValue()).sum();

		if (existingDayHours + request.getHoursWorked().doubleValue() > MAX_DAILY_HOURS) {
			throw new TimesheetException("Total hours for " + request.getWorkDate() + " would exceed " + MAX_DAILY_HOURS
					+ " hours. " + "Currently logged: " + existingDayHours + " hours.");
		}

		// Step 7: Save entry
		TimesheetEntry entry = TimesheetEntry.builder().timesheet(timesheet).workDate(request.getWorkDate())
				.project(project).hoursWorked(request.getHoursWorked()).taskSummary(request.getTaskSummary()).build();

		TimesheetEntry saved = entryRepo.save(entry);

		// Step 8: Update total hours on timesheet
		Double totalHours = entryRepo.sumHoursByTimesheetId(timesheet.getId());
		timesheet.setTotalHours(totalHours);
		timesheetRepo.save(timesheet);

		log.info("Entry added: employee={}, date={}, project={}, hours={}", employeeId, request.getWorkDate(),
				project.getProjectName(), request.getHoursWorked());

		return mapToEntryResponse(saved);
	}
	public WeeklyTimesheetResponse getWeeklyTimesheet(Long employeeId, LocalDate weekStart) {
		Timesheet timesheet = timesheetRepo.findByEmployeeIdAndWeekStartDate(employeeId, weekStart)
				.orElseThrow(() -> new TimesheetException("No timesheet found for week starting: " + weekStart));

		List<TimesheetEntry> entries = entryRepo.findByTimesheetId(timesheet.getId());

		return mapToWeeklyResponse(timesheet, entries);
	}

	@Transactional
	public String submitTimesheet(Long employeeId, String employeeName, SubmitTimesheetRequest request) {

		// Step 1: Find the timesheet
		Timesheet timesheet = timesheetRepo.findByEmployeeIdAndWeekStartDate(employeeId, request.getWeekStartDate())
				.orElseThrow(
						() -> new TimesheetException("Timesheet not found for week: " + request.getWeekStartDate()));

		// Step 2: Already submitted?
		if (timesheet.getStatus() != TimesheetStatus.DRAFT) {
			throw new TimesheetException("Timesheet is already " + timesheet.getStatus() + ". Cannot resubmit.");
		}

		// Step 3: Get all entries
		List<TimesheetEntry> entries = entryRepo.findByTimesheetId(timesheet.getId());

		if (entries.isEmpty()) {
			throw new TimesheetException("Cannot submit an empty timesheet. " + "Please add work entries first.");
		}

		// Step 4: Check all working days are covered (Mon to Fri)
		List<DayOfWeek> workDays = List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
				DayOfWeek.FRIDAY);

		List<DayOfWeek> loggedDays = entries.stream().map(e -> e.getWorkDate().getDayOfWeek()).distinct()
				.collect(Collectors.toList());

		List<String> missingDays = workDays.stream().filter(day -> !loggedDays.contains(day)).map(DayOfWeek::name)
				.collect(Collectors.toList());

		if (!missingDays.isEmpty()) {
			throw new TimesheetException("Missing entries for: " + String.join(", ", missingDays)
					+ ". All working days must have at least one entry.");
		}

		// Step 5: Check total hours
		double totalHours = entries.stream().mapToDouble(e -> e.getHoursWorked().doubleValue()).sum();

		if (totalHours > MAX_WEEKLY_HOURS) {
			throw new TimesheetException(
					"Total hours (" + totalHours + ") exceed the weekly " + "limit of " + MAX_WEEKLY_HOURS + " hours.");
		}

		// Step 6: Update status to SUBMITTED
		timesheet.setStatus(TimesheetStatus.SUBMITTED);
		timesheet.setTotalHours(totalHours);
		timesheet.setSubmittedAt(java.time.LocalDateTime.now());
		timesheetRepo.save(timesheet);

		log.info("Timesheet submitted: employee={}, week={}, hours={}", employeeId, request.getWeekStartDate(),
				totalHours);

		// Step 7: Publish RabbitMQ event
		eventPublisher.publishTimesheetSubmitted(employeeId, employeeName, request.getWeekStartDate(), totalHours);

		return "Timesheet submitted successfully for week of " + request.getWeekStartDate() + ". Total hours: "
				+ totalHours;
	}
	public List<WeeklyTimesheetResponse> getAllTimesheets(Long employeeId) {
		return timesheetRepo.findByEmployeeIdOrderByWeekStartDateDesc(employeeId).stream().map(ts -> {
			List<TimesheetEntry> entries = entryRepo.findByTimesheetId(ts.getId());
			return mapToWeeklyResponse(ts, entries);
		}).collect(Collectors.toList());
	}
	public List<Project> getActiveProjects() {
		return projectRepo.findByIsActiveTrue();
	}

	private Timesheet createNewTimesheet(Long employeeId, String employeeName, LocalDate weekStart) {
		Timesheet ts = Timesheet.builder().employeeId(employeeId).employeeName(employeeName).weekStartDate(weekStart)
				.status(TimesheetStatus.DRAFT).totalHours(0.0).build();
		return timesheetRepo.save(ts);
	}

	private TimesheetEntryResponse mapToEntryResponse(TimesheetEntry e) {
		return TimesheetEntryResponse.builder().id(e.getId()).workDate(e.getWorkDate())
				.projectId(e.getProject().getId()).projectName(e.getProject().getProjectName())
				.hoursWorked(e.getHoursWorked()).taskSummary(e.getTaskSummary()).build();
	}

	private WeeklyTimesheetResponse mapToWeeklyResponse(Timesheet ts, List<TimesheetEntry> entries) {
		return WeeklyTimesheetResponse.builder().timesheetId(ts.getId()).weekStartDate(ts.getWeekStartDate())
				.status(ts.getStatus().name()).totalHours(ts.getTotalHours()).managerComment(ts.getManagerComment())
				.entries(entries.stream().map(this::mapToEntryResponse).collect(Collectors.toList())).build();
	}
}