package com.application.leave.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveTypeResponse {
    private Long    id;
    private String  typeCode;
    private String  typeName;
    private Integer maxDays;
}
