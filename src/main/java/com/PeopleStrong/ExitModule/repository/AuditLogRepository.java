package com.PeopleStrong.ExitModule.repository;

import com.PeopleStrong.ExitModule.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByExitRequest_RequestIdOrderByTimestampDesc(Long requestId);
}
