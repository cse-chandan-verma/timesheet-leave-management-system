package com.application.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.application.admin.entity.AdminNotification;

import java.util.List;

@Repository
public interface AdminNotificationRepository
        extends JpaRepository<AdminNotification, Long> {

    List<AdminNotification> findByEventTypeOrderByReceivedAtDesc(String eventType);

    List<AdminNotification> findByUserEmailOrderByReceivedAtDesc(String userEmail);
}
