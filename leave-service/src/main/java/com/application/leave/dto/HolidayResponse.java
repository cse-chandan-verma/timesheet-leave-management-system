package com.application.leave.dto;

import lombok.*;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HolidayResponse {
    private Long      id;
    private LocalDate holidayDate;
    private String    holidayName;
    private String    description;
}
