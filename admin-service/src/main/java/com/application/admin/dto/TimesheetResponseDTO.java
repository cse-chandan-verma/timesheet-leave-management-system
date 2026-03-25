package com.application.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimesheetResponseDTO {
    private Long id;
    private String employeeEmail;
    private LocalDate weekStartDate;
    private String status;
    private String remarks;
    private Double totalHours;
}
