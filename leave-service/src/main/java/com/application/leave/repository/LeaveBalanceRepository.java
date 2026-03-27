package com.application.leave.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.application.leave.entity.LeaveBalance;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveBalanceRepository
        extends JpaRepository<LeaveBalance, Long> {

    // Get a specific leave type balance for employee in a year
    Optional<LeaveBalance> findByEmployeeIdAndLeaveTypeIdAndYear(
            Long employeeId, Long leaveTypeId, Integer year);

    // Get all balances for an employee in a year
    List<LeaveBalance> findByEmployeeIdAndYear(
            Long employeeId, Integer year);

    boolean existsByEmployeeIdAndYear(Long employeeId, Integer year);
}