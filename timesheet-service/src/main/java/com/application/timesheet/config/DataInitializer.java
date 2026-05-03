package com.application.timesheet.config;

import com.application.timesheet.entity.Project;
import com.application.timesheet.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ProjectRepository projectRepository;

    @Override
    public void run(String... args) {
        if (projectRepository.count() == 0) {
            log.info("Seeding default projects...");
            List<Project> defaultProjects = List.of(
                Project.builder().projectName("Internal Admin").projectCode("INT-001").isActive(true).build(),
                Project.builder().projectName("Client Alpha - Phase 1").projectCode("ALP-101").isActive(true).build(),
                Project.builder().projectName("Client Beta - Support").projectCode("BET-202").isActive(true).build(),
                Project.builder().projectName("Research & Development").projectCode("RD-500").isActive(true).build()
            );
            projectRepository.saveAll(defaultProjects);
            log.info("Seeded {} default projects.", defaultProjects.size());
        } else {
            log.info("Projects already exist, skipping seeding.");
        }
    }
}
