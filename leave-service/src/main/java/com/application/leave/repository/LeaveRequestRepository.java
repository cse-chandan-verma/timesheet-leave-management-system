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

    // All leave requests for an employee, newest first
    List<LeaveRequest> findByEmployeeIdOrderByAppliedAtDesc(Long employeeId);

    // All requests of a given status (used for manager approval queue)
    List<LeaveRequest> findByStatus(LeaveStatus status);

    // Employee's requests filtered by status
    List<LeaveRequest> findByEmployeeIdAndStatus(Long employeeId, LeaveStatus status);

    /**
     * Checks whether the employee already has an ACTIVE (SUBMITTED or APPROVED)
     * leave that overlaps with the requested date range.
     *
     * FIX: Statuses passed as a @Param list — NOT as string literals inside the
     * JPQL string. String literals like 'SUBMITTED' are non-standard JPQL for
     * enum fields and are unreliable across JPA providers. Passing them as
     * typed parameters is the correct and safe approach.
     */
    @Query("SELECT COUNT(l) > 0 FROM LeaveRequest l " +
           "WHERE l.employeeId = :employeeId " +
           "AND l.status IN :statuses " +
           "AND l.fromDate <= :toDate " +
           "AND l.toDate >= :fromDate")
    boolean existsOverlappingLeave(
            @Param("employeeId") Long employeeId,
            @Param("fromDate")   LocalDate fromDate,
            @Param("toDate")     LocalDate toDate,
            @Param("statuses")   List<LeaveStatus> statuses);
}