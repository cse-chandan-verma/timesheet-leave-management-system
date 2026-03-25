package com.application.leave.dto;


import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

/**
 * CLIENT SENDS THIS when applying for leave.
 */
@Data
public class LeaveRequestDto {

    @NotNull(message = "Leave type ID is required")
    private Long leaveTypeId;

    @NotNull(message = "From date is required")
    private LocalDate fromDate;

    @NotNull(message = "To date is required")
    private LocalDate toDate;

    @NotBlank(message = "Reason is required")
    @Size(min = 10, max = 500,
          message = "Reason must be between 10 and 500 characters")
    private String reason;
}
