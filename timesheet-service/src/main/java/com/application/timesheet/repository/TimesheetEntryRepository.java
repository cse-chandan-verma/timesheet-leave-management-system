package com.application.timesheet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.application.timesheet.entity.TimesheetEntry;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimesheetEntryRepository extends JpaRepository<TimesheetEntry, Long> {

	List<TimesheetEntry> findByTimesheetId(Long timesheetId);

	boolean existsByTimesheetIdAndWorkDateAndProjectId(Long timesheetId, LocalDate workDate, Long projectId);

	Optional<TimesheetEntry> findByTimesheetIdAndWorkDateAndProjectId(Long timesheetId, LocalDate workDate,
			Long projectId);

	@Query("SELECT COALESCE(SUM(e.hoursWorked), 0) " + "FROM TimesheetEntry e " + "WHERE e.timesheet.id = :timesheetId")
	Double sumHoursByTimesheetId(@Param("timesheetId") Long timesheetId);

	List<TimesheetEntry> findByTimesheetIdAndWorkDate(Long timesheetId, LocalDate workDate);
}
