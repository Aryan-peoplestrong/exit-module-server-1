package com.PeopleStrong.ExitModule.service;

import com.PeopleStrong.ExitModule.model.AuditLog;
import com.PeopleStrong.ExitModule.model.Employee;
import com.PeopleStrong.ExitModule.model.ExitRequest;
import com.PeopleStrong.ExitModule.model.enums.RequestStatus;
import com.PeopleStrong.ExitModule.dto.ApprovalRequestDto;
import com.PeopleStrong.ExitModule.dto.CreateExitRequestDto;
import com.PeopleStrong.ExitModule.dto.ExitRequestDto;
import com.PeopleStrong.ExitModule.dto.AuditHistoryDto;
import com.PeopleStrong.ExitModule.model.ItChecklist;
import com.PeopleStrong.ExitModule.model.enums.ChecklistStatus;
import com.PeopleStrong.ExitModule.exception.CooldownActiveException;
import com.PeopleStrong.ExitModule.exception.InvalidStateTransitionException;
import com.PeopleStrong.ExitModule.exception.ResourceNotFoundException;
import com.PeopleStrong.ExitModule.common.ExitRequestExceptionMessages;
import com.PeopleStrong.ExitModule.repository.AuditLogRepository;
import com.PeopleStrong.ExitModule.repository.EmployeeRepository;
import com.PeopleStrong.ExitModule.repository.ExitRequestRepository;
import com.PeopleStrong.ExitModule.repository.ItChecklistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExitRequestService {

    private final ExitRequestRepository exitRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final AuditLogRepository auditLogRepository;
    private final ItChecklistRepository itChecklistRepository;

    @Transactional
    public ExitRequestDto applyForExit(String userEmail, CreateExitRequestDto request) {
        log.info("Processing exit request for user: {}", userEmail);
        Employee employee = employeeRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException(ExitRequestExceptionMessages.EMPLOYEE_NOT_FOUND));

        if (employee.getCooldownUntil() != null && employee.getCooldownUntil().isAfter(LocalDate.now())) {
            log.warn("Cooldown active for empId: {} until {}", employee.getEmpId(), employee.getCooldownUntil());
            throw new CooldownActiveException(
                    ExitRequestExceptionMessages.COOLDOWN_ACTIVE_PREFIX + employee.getCooldownUntil());
        }

        // Check if there's already an active request
        boolean hasActive = exitRequestRepository.findByEmployee_EmpId(employee.getEmpId()).stream()
                .anyMatch(r -> r.getStatus() != RequestStatus.REJECTED && r.getStatus() != RequestStatus.SUCCESS);

        if (hasActive) {
            log.warn("Active exit request already exists for empId: {}", employee.getEmpId());
            throw new InvalidStateTransitionException(ExitRequestExceptionMessages.ACTIVE_REQUEST_EXISTS);
        }

        ExitRequest exitRequest = ExitRequest.builder()
                .employee(employee)
                .dateOfLeaving(request.getDateOfLeaving())
                .reason(request.getReason())
                .status(RequestStatus.PENDING_L1)
                .build();

        exitRequestRepository.save(exitRequest);
        log.info("Exit request created with requestId: {} for empId: {}", exitRequest.getRequestId(), employee.getEmpId());
        logAudit(exitRequest, employee, ExitRequestExceptionMessages.AUDIT_ACTION_SUBMITTED, ExitRequestExceptionMessages.AUDIT_COMMENT_SUBMITTED);

        return mapToDto(exitRequest);
    }

    public List<ExitRequestDto> getMyRequests(String userEmail) {
        log.debug("Fetching exit requests for user: {}", userEmail);
        Employee employee = employeeRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException(ExitRequestExceptionMessages.EMPLOYEE_NOT_FOUND));
        return exitRequestRepository.findByEmployee_EmpId(employee.getEmpId())
                .stream()
                .sorted(Comparator.comparing(ExitRequest::getRequestId, Comparator.reverseOrder()))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<ExitRequestDto> getRequestsForL1(String managerEmail) {
        log.debug("Fetching L1 requests for manager: {}", managerEmail);
        Employee manager = employeeRepository.findByEmail(managerEmail)
                .orElseThrow(() -> new ResourceNotFoundException(ExitRequestExceptionMessages.MANAGER_NOT_FOUND));
        return exitRequestRepository.findByEmployee_L1Manager_EmpId(manager.getEmpId())
                .stream()
                .sorted(Comparator.comparing((ExitRequest r) -> r.getStatus() == RequestStatus.PENDING_L1 ? 0 : 1)
                        .thenComparing(ExitRequest::getRequestId, Comparator.reverseOrder()))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ExitRequestDto l1Approve(String managerEmail, Long requestId) {
        log.info("L1 approving requestId: {} by manager: {}", requestId, managerEmail);
        ExitRequest request = getAndValidateRequestForManager(managerEmail, requestId, true);
        if (request.getStatus() != RequestStatus.PENDING_L1) {
            throw new InvalidStateTransitionException(ExitRequestExceptionMessages.NOT_IN_PENDING_L1);
        }
        request.setStatus(RequestStatus.PENDING_HR);
        exitRequestRepository.save(request);

        Employee manager = employeeRepository.findByEmail(managerEmail).get();
        logAudit(request, manager, ExitRequestExceptionMessages.AUDIT_ACTION_APPROVED_L1, ExitRequestExceptionMessages.AUDIT_COMMENT_APPROVED_L1);
        log.info("Request {} moved to PENDING_HR after L1 approval", requestId);
        return mapToDto(request);
    }

    @Transactional
    public ExitRequestDto l1Reject(String managerEmail, Long requestId, ApprovalRequestDto approval) {
        log.info("L1 rejecting requestId: {} by manager: {}", requestId, managerEmail);
        if (approval.getComments() == null || approval.getComments().isBlank()) {
            throw new IllegalArgumentException(ExitRequestExceptionMessages.REJECTION_COMMENTS_MANDATORY);
        }
        ExitRequest request = getAndValidateRequestForManager(managerEmail, requestId, true);
        if (request.getStatus() != RequestStatus.PENDING_L1) {
            throw new InvalidStateTransitionException(ExitRequestExceptionMessages.NOT_IN_PENDING_L1);
        }

        applyRejection(request, approval.getComments());
        Employee manager = employeeRepository.findByEmail(managerEmail).get();
        logAudit(request, manager, ExitRequestExceptionMessages.AUDIT_ACTION_REJECTED_L1, approval.getComments());
        log.info("Request {} rejected by L1, cooldown applied for empId: {}", requestId, request.getEmployee().getEmpId());
        return mapToDto(request);
    }

    public List<ExitRequestDto> getRequestsForHr(String hrEmail) {
        log.debug("Fetching HR requests for manager: {}", hrEmail);
        Employee hrManager = employeeRepository.findByEmail(hrEmail)
                .orElseThrow(() -> new ResourceNotFoundException(ExitRequestExceptionMessages.HR_NOT_FOUND));
        return exitRequestRepository.findByEmployee_HrManager_EmpId(hrManager.getEmpId())
                .stream()
                .sorted(Comparator.comparing((ExitRequest r) -> r.getStatus() == RequestStatus.PENDING_HR ? 0 : 1)
                        .thenComparing(ExitRequest::getRequestId, Comparator.reverseOrder()))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ExitRequestDto hrApprove(String hrEmail, Long requestId) {
        log.info("HR approving requestId: {} by manager: {}", requestId, hrEmail);
        ExitRequest request = getAndValidateRequestForManager(hrEmail, requestId, false);
        if (request.getStatus() != RequestStatus.PENDING_HR) {
            throw new InvalidStateTransitionException(ExitRequestExceptionMessages.NOT_IN_PENDING_HR);
        }
        request.setStatus(RequestStatus.PENDING_IT_CLEARANCE);
        exitRequestRepository.save(request);

        ItChecklist initialChecklist = ItChecklist.builder()
                .exitRequest(request)
                .iteration(1)
                .status(ChecklistStatus.PENDING)
                .build();
        itChecklistRepository.save(initialChecklist);
        log.info("Request {} moved to PENDING_IT_CLEARANCE, IT checklist created", requestId);

        Employee hrManager = employeeRepository.findByEmail(hrEmail).get();
        logAudit(request, hrManager, ExitRequestExceptionMessages.AUDIT_ACTION_APPROVED_HR, ExitRequestExceptionMessages.AUDIT_COMMENT_APPROVED_HR);

        return mapToDto(request);
    }

    @Transactional
    public ExitRequestDto hrReject(String hrEmail, Long requestId, ApprovalRequestDto approval) {
        log.info("HR rejecting requestId: {} by manager: {}", requestId, hrEmail);
        if (approval.getComments() == null || approval.getComments().isBlank()) {
            throw new IllegalArgumentException(ExitRequestExceptionMessages.REJECTION_COMMENTS_MANDATORY);
        }
        ExitRequest request = getAndValidateRequestForManager(hrEmail, requestId, false);
        if (request.getStatus() != RequestStatus.PENDING_HR) {
            throw new InvalidStateTransitionException(ExitRequestExceptionMessages.NOT_IN_PENDING_HR);
        }

        applyRejection(request, approval.getComments());
        Employee hrManager = employeeRepository.findByEmail(hrEmail).get();
        logAudit(request, hrManager, ExitRequestExceptionMessages.AUDIT_ACTION_REJECTED_HR, approval.getComments());
        log.info("Request {} rejected by HR, cooldown applied for empId: {}", requestId, request.getEmployee().getEmpId());
        return mapToDto(request);
    }

    private void applyRejection(ExitRequest request, String reason) {
        request.setStatus(RequestStatus.REJECTED);
        request.setRejectionReason(reason);
        request.getEmployee().setCooldownUntil(LocalDate.now().plusDays(30));
        employeeRepository.save(request.getEmployee());
        exitRequestRepository.save(request);
        log.debug("Rejection applied to requestId: {}, cooldown set until: {}", request.getRequestId(), request.getEmployee().getCooldownUntil());
    }

    private ExitRequest getAndValidateRequestForManager(String email, Long requestId, boolean isL1) {
        ExitRequest request = exitRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException(ExitRequestExceptionMessages.EXIT_REQUEST_NOT_FOUND));

        Employee manager = employeeRepository.findByEmail(email).get(); // Auth guarantees this exists

        if (isL1) {
            if (request.getEmployee().getL1Manager() == null
                    || !request.getEmployee().getL1Manager().getEmpId().equals(manager.getEmpId())) {
                log.warn("Manager {} is not the L1 manager for requestId: {}", email, requestId);
                throw new InvalidStateTransitionException(ExitRequestExceptionMessages.NOT_L1_MANAGER);
            }
        } else {
            if (request.getEmployee().getHrManager() == null
                    || !request.getEmployee().getHrManager().getEmpId().equals(manager.getEmpId())) {
                log.warn("Manager {} is not the HR manager for requestId: {}", email, requestId);
                throw new InvalidStateTransitionException(ExitRequestExceptionMessages.NOT_HR_MANAGER);
            }
        }
        return request;
    }

    private ExitRequestDto mapToDto(ExitRequest exitRequest) {
        return ExitRequestDto.builder()
                .requestId(exitRequest.getRequestId())
                .empId(exitRequest.getEmployee().getEmpId())
                .empName(exitRequest.getEmployee().getName())
                .dateOfLeaving(exitRequest.getDateOfLeaving())
                .reason(exitRequest.getReason())
                .status(exitRequest.getStatus())
                .rejectionReason(exitRequest.getRejectionReason())
                .build();
    }

    public List<AuditHistoryDto> getAuditHistoryById(Long requestId) {
        log.debug("Fetching audit history for requestId: {}", requestId);
        ExitRequest request = exitRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException(ExitRequestExceptionMessages.EXIT_REQUEST_NOT_FOUND));

        return auditLogRepository.findByExitRequest_RequestIdOrderByTimestampDesc(requestId)
                .stream()
                .map(auditLog -> AuditHistoryDto.builder()
                        .actorId(auditLog.getActor() != null ? auditLog.getActor().getEmpId() : null)
                        .actorName(auditLog.getActor() != null ? auditLog.getActor().getName() : ExitRequestExceptionMessages.SYSTEM_ACTOR_NAME)
                        .action(auditLog.getAction())
                        .comments(auditLog.getComments())
                        .timestamp(auditLog.getTimestamp())
                        .build())
                .collect(Collectors.toList());
    }

    private void logAudit(ExitRequest request, Employee actor, String action, String comments) {
        AuditLog auditLog = AuditLog.builder()
                .exitRequest(request)
                .actor(actor)
                .action(action)
                .comments(comments)
                .build();
        auditLogRepository.save(auditLog);
        log.debug("Audit log saved: requestId={}, action={}, actorId={}", request.getRequestId(), action, actor.getEmpId());
    }
}
