package com.PeopleStrong.ExitModule.scheduler;

import com.PeopleStrong.ExitModule.model.AuditLog;
import com.PeopleStrong.ExitModule.model.ExitRequest;
import com.PeopleStrong.ExitModule.model.enums.RequestStatus;
import com.PeopleStrong.ExitModule.repository.AuditLogRepository;
import com.PeopleStrong.ExitModule.repository.EmployeeRepository;
import com.PeopleStrong.ExitModule.repository.ExitRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AutoRejectScheduler {

    private final ExitRequestRepository exitRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final AuditLogRepository auditLogRepository;

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void rejectStaleRequests() {
        log.info("Running auto-reject scheduler for stale requests...");
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);
        
        List<RequestStatus> pendingStatuses = Arrays.asList(RequestStatus.PENDING_L1, RequestStatus.PENDING_HR);
        
        List<ExitRequest> staleRequests = exitRequestRepository.findStaleRequests(pendingStatuses, cutoffDate);
        
        if(staleRequests.isEmpty()) {
            log.info("No stale requests found.");
            return;
        }

        for (ExitRequest request : staleRequests) {
            log.info("Auto-rejecting request ID {}", request.getRequestId());
            request.setStatus(RequestStatus.REJECTED);
            request.setRejectionReason("System Auto-Reject due to 7 days of inactivity.");
            
            request.getEmployee().setCooldownUntil(LocalDate.now().plusDays(30));
            employeeRepository.save(request.getEmployee());
            
            AuditLog auditLog = AuditLog.builder()
                    .exitRequest(request)
                    .action("AUTO_REJECTED")
                    .comments("System auto-rejected request due to inactivity")
                    .build();
            auditLogRepository.save(auditLog);
            
            exitRequestRepository.save(request);
        }
        
        log.info("Successfully rejected {} stale requests.", staleRequests.size());
    }
}
