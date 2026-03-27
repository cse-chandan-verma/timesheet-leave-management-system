package com.application.leave.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.application.leave.entity.Holiday;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    // Fetch all holidays that fall within a date range (used during leave calculation)
    List<Holiday> findByHolidayDateBetween(LocalDate from, LocalDate to);

    // Check if a specific date is already registered as a holiday
    boolean existsByHolidayDate(LocalDate holidayDate);

    Optional<Holiday> findByHolidayDate(LocalDate holidayDate);

    // Get all holidays for a given year (for admin listing)
    List<Holiday> findByHolidayDateBetweenOrderByHolidayDateAsc(LocalDate yearStart, LocalDate yearEnd);
}
