package com.application.leave.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "leave_types")
public class LeaveType {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "type_code", nullable = false, unique = true, length = 20)
	private String typeCode;
	// e.g. "CL", "SL", "EL", "COL"

	@Column(name = "type_name", nullable = false, length = 50)
	private String typeName;
	// e.g. "Casual Leave", "Sick Leave"

	@Column(name = "max_days", nullable = false)
	private Integer maxDays;
	// Maximum days allowed per year for this type

	@Column(name = "is_active", nullable = false)
	@Builder.Default
	private boolean isActive = true;
}