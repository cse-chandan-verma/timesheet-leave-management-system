package com.application.leave.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.application.leave.entity.LeaveType;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveTypeRepository
        extends JpaRepository<LeaveType, Long> {

    List<LeaveType> findByIsActiveTrue();

    Optional<LeaveType> findByTypeCode(String typeCode);
}