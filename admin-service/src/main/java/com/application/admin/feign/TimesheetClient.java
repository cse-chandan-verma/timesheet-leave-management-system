package com.application.admin.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.application.admin.dto.ApprovalRequestDTO;
import com.application.admin.dto.TimesheetResponseDTO;

import java.util.List;

/**
 * Feign client to communicate with timesheet-service.
 * Service name and endpoints are aligned with the new refactored architecture.
 * Hardcoded URL removed for automatic Eureka discovery in Docker.
 */
@FeignClient(name = "timesheet-service")
public interface TimesheetClient {

    @GetMapping("/timesheet/admin/submitted")
    List<TimesheetResponseDTO> getPendingTimesheets();

    @PutMapping("/timesheet/admin/approve/{timesheetId}")
    String approveTimesheet(
            @PathVariable("timesheetId") Long timesheetId,
            @RequestBody ApprovalRequestDTO request);

    @PutMapping("/timesheet/admin/reject/{timesheetId}")
    String rejectTimesheet(
            @PathVariable("timesheetId") Long timesheetId,
            @RequestBody ApprovalRequestDTO request);
}
