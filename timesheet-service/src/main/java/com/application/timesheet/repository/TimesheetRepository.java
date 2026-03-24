package com.application.timesheet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.application.timesheet.entity.Timesheet;
import com.application.timesheet.entity.Timesheet.TimesheetStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimesheetRepository extends JpaRepository<Timesheet, Long> {

	Optional<Timesheet> findByEmployeeIdAndWeekStartDate(Long employeeId, LocalDate weekStartDate);

	List<Timesheet> findByEmployeeIdOrderByWeekStartDateDesc(Long employeeId);

	List<Timesheet> findByStatus(TimesheetStatus status);

	boolean existsByEmployeeIdAndWeekStartDate(Long employeeId, LocalDate weekStartDate);
}
