package com.PeopleStrong.ExitModule.service;

import com.PeopleStrong.ExitModule.model.AuditLog;
import com.PeopleStrong.ExitModule.model.Employee;
import com.PeopleStrong.ExitModule.model.ExitRequest;
import com.PeopleStrong.ExitModule.model.enums.RequestStatus;
import com.PeopleStrong.ExitModule.dto.ApprovalRequestDto;
import com.PeopleStrong.ExitModule.dto.CreateExitRequestDto;
import com.PeopleStrong.ExitModule.dto.ExitRequestDto;
import com.PeopleStrong.ExitModule.model.ItChecklist;
import com.PeopleStrong.ExitModule.model.enums.ChecklistStatus;
import com.PeopleStrong.ExitModule.exception.CooldownActiveException;
import com.PeopleStrong.ExitModule.exception.InvalidStateTransitionException;
import com.PeopleStrong.ExitModule.exception.ResourceNotFoundException;
import com.PeopleStrong.ExitModule.repository.AuditLogRepository;
import com.PeopleStrong.ExitModule.repository.EmployeeRepository;
import com.PeopleStrong.ExitModule.repository.ExitRequestRepository;
import com.PeopleStrong.ExitModule.repository.ItChecklistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExitRequestService {

    private final ExitRequestRepository exitRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final AuditLogRepository auditLogRepository;
    private final ItChecklistRepository itChecklistRepository;

    @Transactional
    public ExitRequestDto applyForExit(String userEmail, CreateExitRequestDto request) {
        Employee employee = employeeRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        if (employee.getCooldownUntil() != null && employee.getCooldownUntil().isAfter(LocalDate.now())) {
            throw new CooldownActiveException("Cannot submit request. Cooldown active until " + employee.getCooldownUntil());
        }

        // Check if there's already an active request
        boolean hasActive = exitRequestRepository.findByEmployee_EmpId(employee.getEmpId()).stream()
                .anyMatch(r -> r.getStatus() != RequestStatus.REJECTED && r.getStatus() != RequestStatus.SUCCESS);

        if (hasActive) {
            throw new InvalidStateTransitionException("An active exit request already exists.");
        }

        ExitRequest exitRequest = ExitRequest.builder()
                .employee(employee)
                .dateOfLeaving(request.getDateOfLeaving())
                .reason(request.getReason())
                .status(RequestStatus.PENDING_L1)
                .build();

        exitRequestRepository.save(exitRequest);
        logAudit(exitRequest, employee, "SUBMITTED", "Employee submitted exit request");

        return mapToDto(exitRequest);
    }

    public List<ExitRequestDto> getMyRequests(String userEmail) {
        Employee employee = employeeRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        return exitRequestRepository.findByEmployee_EmpId(employee.getEmpId())
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public List<ExitRequestDto> getRequestsForL1(String managerEmail) {
        Employee manager = employeeRepository.findByEmail(managerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found"));
        return exitRequestRepository.findByEmployee_L1Manager_EmpId(manager.getEmpId())
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional
    public ExitRequestDto l1Approve(String managerEmail, Long requestId) {
        ExitRequest request = getAndValidateRequestForManager(managerEmail, requestId, true);
        if (request.getStatus() != RequestStatus.PENDING_L1) {
            throw new InvalidStateTransitionException("Request is not in PENDING_L1 state");
        }
        request.setStatus(RequestStatus.PENDING_HR);
        exitRequestRepository.save(request);
        
        Employee manager = employeeRepository.findByEmail(managerEmail).get();
        logAudit(request, manager, "APPROVED_L1", "L1 Manager approved");
        return mapToDto(request);
    }

    @Transactional
    public ExitRequestDto l1Reject(String managerEmail, Long requestId, ApprovalRequestDto approval) {
        if (approval.getComments() == null || approval.getComments().isBlank()) {
            throw new IllegalArgumentException("Rejection comments are mandatory");
        }
        ExitRequest request = getAndValidateRequestForManager(managerEmail, requestId, true);
        if (request.getStatus() != RequestStatus.PENDING_L1) {
            throw new InvalidStateTransitionException("Request is not in PENDING_L1 state");
        }

        applyRejection(request, approval.getComments());
        Employee manager = employeeRepository.findByEmail(managerEmail).get();
        logAudit(request, manager, "REJECTED_L1", approval.getComments());
        return mapToDto(request);
    }

    public List<ExitRequestDto> getRequestsForHr(String hrEmail) {
        Employee hrManager = employeeRepository.findByEmail(hrEmail)
                .orElseThrow(() -> new ResourceNotFoundException("HR not found"));
        return exitRequestRepository.findByEmployee_HrManager_EmpId(hrManager.getEmpId())
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional
    public ExitRequestDto hrApprove(String hrEmail, Long requestId) {
        ExitRequest request = getAndValidateRequestForManager(hrEmail, requestId, false);
        if (request.getStatus() != RequestStatus.PENDING_HR) {
            throw new InvalidStateTransitionException("Request is not in PENDING_HR state");
        }
        request.setStatus(RequestStatus.PENDING_IT_CLEARANCE);
        exitRequestRepository.save(request);

        ItChecklist initialChecklist = ItChecklist.builder()
                .exitRequest(request)
                .iteration(1)
                .status(ChecklistStatus.PENDING)
                .build();
        itChecklistRepository.save(initialChecklist);

        Employee hrManager = employeeRepository.findByEmail(hrEmail).get();
        logAudit(request, hrManager, "APPROVED_HR", "HR Manager approved. Pending IT checklist.");
        
        return mapToDto(request);
    }

    @Transactional
    public ExitRequestDto hrReject(String hrEmail, Long requestId, ApprovalRequestDto approval) {
        if (approval.getComments() == null || approval.getComments().isBlank()) {
            throw new IllegalArgumentException("Rejection comments are mandatory");
        }
        ExitRequest request = getAndValidateRequestForManager(hrEmail, requestId, false);
        if (request.getStatus() != RequestStatus.PENDING_HR) {
            throw new InvalidStateTransitionException("Request is not in PENDING_HR state");
        }

        applyRejection(request, approval.getComments());
        Employee hrManager = employeeRepository.findByEmail(hrEmail).get();
        logAudit(request, hrManager, "REJECTED_HR", approval.getComments());
        return mapToDto(request);
    }

    private void applyRejection(ExitRequest request, String reason) {
        request.setStatus(RequestStatus.REJECTED);
        request.setRejectionReason(reason);
        request.getEmployee().setCooldownUntil(LocalDate.now().plusDays(30));
        employeeRepository.save(request.getEmployee());
        exitRequestRepository.save(request);
    }

    private ExitRequest getAndValidateRequestForManager(String email, Long requestId, boolean isL1) {
        ExitRequest request = exitRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Exit Request not found"));
        
        Employee manager = employeeRepository.findByEmail(email).get(); // Auth guarantees this exists
        
        if (isL1) {
            if (request.getEmployee().getL1Manager() == null || !request.getEmployee().getL1Manager().getEmpId().equals(manager.getEmpId())) {
                throw new InvalidStateTransitionException("You are not the L1 Manager for this employee.");
            }
        } else {
            if (request.getEmployee().getHrManager() == null || !request.getEmployee().getHrManager().getEmpId().equals(manager.getEmpId())) {
                throw new InvalidStateTransitionException("You are not the HR Manager for this employee.");
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

    private void logAudit(ExitRequest request, Employee actor, String action, String comments) {
        AuditLog auditLog = AuditLog.builder()
                .exitRequest(request)
                .actor(actor)
                .action(action)
                .comments(comments)
                .build();
        auditLogRepository.save(auditLog);
    }
}
