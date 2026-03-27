package com.application.timesheet.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateEntryRequest {

    @NotNull(message = "Hours worked is required")
    @DecimalMin(value = "0.5", message = "Minimum 0.5 hours per entry")
    @DecimalMax(value = "12.0", message = "Cannot exceed 12 hours per entry")
    private BigDecimal hoursWorked;

    @Size(max = 500, message = "Task summary cannot exceed 500 characters")
    private String taskSummary;
}
