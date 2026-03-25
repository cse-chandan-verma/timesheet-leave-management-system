package com.application.leave.dto;


import lombok.*;

/**
 * SERVER RETURNS THIS when employee checks leave balance.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveBalanceResponse {
    private Long   leaveTypeId;
    private String leaveTypeCode;
    private String leaveTypeName;
    private Integer totalDays;
    private Integer usedDays;
    private Integer remainingDays;
    private Integer year;
}
