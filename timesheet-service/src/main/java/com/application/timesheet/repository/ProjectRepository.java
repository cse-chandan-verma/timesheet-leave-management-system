package com.application.timesheet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.application.timesheet.entity.Project;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

	List<Project> findByIsActiveTrue();

	Optional<Project> findByProjectCode(String projectCode);

	boolean existsByProjectCode(String projectCode);
}
