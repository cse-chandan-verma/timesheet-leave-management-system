package com.application.admin.feign;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.application.admin.dto.LeaveResponseDTO;

import java.util.List;

@FeignClient(name = "leave-service", url = "http://localhost:8083")
public interface LeaveClient {

    @GetMapping("/leave/pending")
    List<LeaveResponseDTO> getPendingLeaveRequests(
            @RequestParam("email") String email,
            @RequestParam("role") String role);

    @PutMapping("/leave/{id}/approve")
    LeaveResponseDTO approveLeave(
            @PathVariable("id") Long id,
            @RequestParam("email") String email,
            @RequestParam("role") String role);

    @PutMapping("/leave/{id}/reject")
    LeaveResponseDTO rejectLeave(
            @PathVariable("id") Long id,
            @RequestParam("remarks") String remarks,
            @RequestParam("email") String email,
            @RequestParam("role") String role);
}
