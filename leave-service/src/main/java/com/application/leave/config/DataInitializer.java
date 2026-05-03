package com.application.leave.config;

import com.application.leave.entity.LeaveType;
import com.application.leave.repository.LeaveTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final LeaveTypeRepository leaveTypeRepository;

    @Override
    public void run(String... args) {
        if (leaveTypeRepository.count() == 0) {
            log.info("Seeding default leave types...");
            List<LeaveType> defaultTypes = List.of(
                LeaveType.builder().typeName("Casual Leave").typeCode("CL").maxDays(15).isActive(true).build(),
                LeaveType.builder().typeName("Sick Leave").typeCode("SL").maxDays(12).isActive(true).build(),
                LeaveType.builder().typeName("Earned Leave").typeCode("EL").maxDays(15).isActive(true).build(),
                LeaveType.builder().typeName("Compensatory Off").typeCode("COL").maxDays(10).isActive(true).build()
            );
            leaveTypeRepository.saveAll(defaultTypes);
            log.info("Seeded {} default leave types.", defaultTypes.size());
        } else {
            log.info("Leave types already exist, skipping seeding.");
        }
    }
}
