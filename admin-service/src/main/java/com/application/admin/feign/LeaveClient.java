package com.application.admin.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.application.admin.dto.ApprovalRequestDTO;
import com.application.admin.dto.LeaveResponseDTO;

import java.util.List;

/**
 * Feign client to communicate with leave-service.
 * Service name and endpoints are aligned with the new refactored architecture.
 */
@FeignClient(name = "leave-service")
public interface LeaveClient {

    @GetMapping("/leave/admin/pending")
    List<LeaveResponseDTO> getPendingLeaveRequests();

    @PutMapping("/leave/admin/approve/{id}")
    String approveLeave(
            @PathVariable("id") Long id,
            @RequestBody ApprovalRequestDTO request);

    @PutMapping("/leave/admin/reject/{id}")
    String rejectLeave(
            @PathVariable("id") Long id,
            @RequestBody ApprovalRequestDTO request);
}
