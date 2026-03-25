package com.application.leave.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.application.leave.entity.LeaveRequest;
import com.application.leave.entity.LeaveRequest.LeaveStatus;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRequestRepository
        extends JpaRepository<LeaveRequest, Long> {

    // All leave requests for an employee
    List<LeaveRequest> findByEmployeeIdOrderByAppliedAtDesc(
            Long employeeId);

    // All pending requests (for manager approval queue)
    List<LeaveRequest> findByStatus(LeaveStatus status);

    @Query("SELECT COUNT(l) > 0 FROM LeaveRequest l " +
           "WHERE l.employeeId = :employeeId " +
           "AND l.status IN ('SUBMITTED', 'APPROVED') " +
           "AND l.fromDate <= :toDate " +
           "AND l.toDate >= :fromDate")
    boolean existsOverlappingLeave(
            @Param("employeeId") Long employeeId,
            @Param("fromDate")   LocalDate fromDate,
            @Param("toDate")     LocalDate toDate);

    // Employee's requests by status
    List<LeaveRequest> findByEmployeeIdAndStatus(
            Long employeeId, LeaveStatus status);
}