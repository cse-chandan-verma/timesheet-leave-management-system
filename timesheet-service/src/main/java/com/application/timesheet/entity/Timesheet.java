package com.application.timesheet.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "timesheets",
    uniqueConstraints = {
        @UniqueConstraint(
            columnNames = {"employee_id", "week_start_date"},
            name = "uk_employee_week"
        )
    }
)
public class Timesheet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;
    

    @Column(name = "employee_name", length = 100)
    private String employeeName;

    @Column(name = "week_start_date", nullable = false)
    private LocalDate weekStartDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TimesheetStatus status = TimesheetStatus.DRAFT;

    @Column(name = "total_hours")
    @Builder.Default
    private Double totalHours = 0.0;

    @Column(name = "manager_comment", length = 500)
    private String managerComment;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum TimesheetStatus {
        DRAFT,      // Entries saved but not submitted
        SUBMITTED,  // Submitted to manager for review
        APPROVED,   // Manager approved
        REJECTED    // Manager rejected — employee must resubmit
    }
}