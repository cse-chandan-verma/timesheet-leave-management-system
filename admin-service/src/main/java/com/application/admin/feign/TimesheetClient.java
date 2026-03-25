package com.application.admin.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.application.admin.dto.TimesheetResponseDTO;

import java.util.List;

@FeignClient(name = "timesheet-service", url = "http://localhost:8082")
public interface TimesheetClient {

    @GetMapping("/timesheet/pending")
    List<TimesheetResponseDTO> getPendingTimesheets(
            @RequestParam("email") String email,
            @RequestParam("role") String role);

    @PutMapping("/timesheet/{id}/approve")
    TimesheetResponseDTO approveTimesheet(
            @PathVariable("id") Long id,
            @RequestParam("email") String email,
            @RequestParam("role") String role);

    @PutMapping("/timesheet/{id}/reject")
    TimesheetResponseDTO rejectTimesheet(
            @PathVariable("id") Long id,
            @RequestParam("remarks") String remarks,
            @RequestParam("email") String email,
            @RequestParam("role") String role);
}
