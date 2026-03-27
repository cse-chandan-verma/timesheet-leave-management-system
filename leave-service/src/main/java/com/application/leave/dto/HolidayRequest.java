package com.application.leave.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class HolidayRequest {

    @NotNull(message = "Holiday date is required")
    private LocalDate holidayDate;

    @NotBlank(message = "Holiday name is required")
    @Size(max = 100, message = "Holiday name cannot exceed 100 characters")
    private String holidayName;

    @Size(max = 300, message = "Description cannot exceed 300 characters")
    private String description;
}
