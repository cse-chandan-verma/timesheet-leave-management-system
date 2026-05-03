package com.application.timesheet.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "auth-service")
public interface AuthServiceClient {

    @GetMapping("/auth/internal/manager/{managerId}/employee-ids")
    List<Long> getTeamEmployeeIds(@PathVariable("managerId") Long managerId);
}
