package com.application.leave.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * PUBLIC HOLIDAY ENTITY
 *
 * Stores company/national public holidays per date.
 * Used by LeaveService to exclude holidays from working-day calculations —
 * so an employee applying for 5 days over a week with a holiday only uses 4 leave days.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "holidays",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"holiday_date"}, name = "uk_holiday_date")
    }
)
public class Holiday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "holiday_date", nullable = false, unique = true)
    private LocalDate holidayDate;

    @Column(name = "holiday_name", nullable = false, length = 100)
    private String holidayName;

    @Column(name = "description", length = 300)
    private String description;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
