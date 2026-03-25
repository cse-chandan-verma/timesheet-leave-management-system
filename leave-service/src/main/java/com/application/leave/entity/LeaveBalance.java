package com.application.leave.entity;


import jakarta.persistence.*;
import lombok.*;

/**
 * LEAVE BALANCE ENTITY
 *
 * Tracks how many days of each leave type
 * an employee has used and has remaining.
 *
 * One record per employee per leave type per year.
 * e.g. Employee 1 has 12 CL days in 2025 — used 3 — remaining 9
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "leave_balances",
    uniqueConstraints = {
        @UniqueConstraint(
            columnNames = {"employee_id", "leave_type_id", "year"},
            name = "uk_emp_type_year"
        )
    }
)
public class LeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @Column(nullable = false)
    private Integer year;
    // e.g. 2025

    @Column(name = "total_days", nullable = false)
    private Integer totalDays;
    // Total days allocated for this year

    @Column(name = "used_days", nullable = false)
    @Builder.Default
    private Integer usedDays = 0;
    // Days already used

    // Calculated field — not stored in DB
    public Integer getRemainingDays() {
        return totalDays - usedDays;
    }
}