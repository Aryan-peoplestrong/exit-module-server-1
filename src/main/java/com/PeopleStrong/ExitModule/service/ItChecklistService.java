package com.PeopleStrong.ExitModule.service;

import com.PeopleStrong.ExitModule.model.AuditLog;
import com.PeopleStrong.ExitModule.model.Employee;
import com.PeopleStrong.ExitModule.model.ExitRequest;
import com.PeopleStrong.ExitModule.model.ItChecklist;
import com.PeopleStrong.ExitModule.model.enums.ChecklistStatus;
import com.PeopleStrong.ExitModule.model.enums.RequestStatus;
import com.PeopleStrong.ExitModule.dto.ApprovalRequestDto;
import com.PeopleStrong.ExitModule.dto.ItChecklistDto;
import com.PeopleStrong.ExitModule.exception.InvalidStateTransitionException;
import com.PeopleStrong.ExitModule.exception.ResourceNotFoundException;
import com.PeopleStrong.ExitModule.repository.AuditLogRepository;
import com.PeopleStrong.ExitModule.repository.EmployeeRepository;
import com.PeopleStrong.ExitModule.repository.ExitRequestRepository;
import com.PeopleStrong.ExitModule.repository.ItChecklistRepository;
import com.PeopleStrong.ExitModule.common.ChecklistMessages;
import com.PeopleStrong.ExitModule.common.ChecklistExceptionMessages;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItChecklistService {

    private final ItChecklistRepository itChecklistRepository;
    private final ExitRequestRepository exitRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final AuditLogRepository auditLogRepository;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    @Transactional
    public ItChecklistDto uploadDocument(String employeeEmail, Long requestId, MultipartFile file) {
        log.info("Processing document upload for requestId: {} by user: {}", requestId, employeeEmail);
        Employee employee = employeeRepository.findByEmail(employeeEmail).get();
        ExitRequest exitRequest = exitRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException(ChecklistExceptionMessages.EXIT_REQUEST_NOT_FOUND));

        if (!exitRequest.getEmployee().getEmpId().equals(employee.getEmpId())) {
            log.warn("Upload denied - user {} does not own requestId: {}", employeeEmail, requestId);
            throw new InvalidStateTransitionException(ChecklistExceptionMessages.NOT_YOUR_REQUEST);
        }

        ItChecklist activeChecklist = itChecklistRepository.findByExitRequest_RequestId(requestId).stream()
                .filter(c -> c.getStatus() == ChecklistStatus.PENDING)
                .findFirst()
                .orElseThrow(() -> new InvalidStateTransitionException(ChecklistExceptionMessages.NO_PENDING_CHECKLIST_FOR_UPLOAD));

        // Save file
        try {
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();

            String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path path = Paths.get(uploadDir, filename);
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
            activeChecklist.setDocumentPath(path.toString());
            itChecklistRepository.save(activeChecklist);
            log.info("Document uploaded successfully: {} for checklistId: {}", filename, activeChecklist.getChecklistId());

            return mapToDto(activeChecklist);
        } catch (IOException e) {
            log.error("Failed to store file for requestId: {} - {}", requestId, e.getMessage(), e);
            throw new RuntimeException(ChecklistExceptionMessages.FILE_STORE_FAILED, e);
        }
    }

    public List<ItChecklistDto> getPendingChecklists() {
        log.debug("Fetching pending IT checklists");
        return itChecklistRepository.findByStatus(ChecklistStatus.PENDING)
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional
    public ItChecklistDto itApprove(String itEmail, Long checklistId) {
        log.info("IT approving checklistId: {} by user: {}", checklistId, itEmail);
        Employee itUser = employeeRepository.findByEmail(itEmail).get();
        ItChecklist checklist = itChecklistRepository.findById(checklistId)
                .orElseThrow(() -> new ResourceNotFoundException(ChecklistExceptionMessages.CHECKLIST_NOT_FOUND));

        if (checklist.getStatus() != ChecklistStatus.PENDING) {
            throw new InvalidStateTransitionException(ChecklistExceptionMessages.CHECKLIST_NOT_PENDING);
        }

        checklist.setIdCardReceived(true);
        checklist.setAccessCardReceived(true);
        checklist.setLaptopReceived(true);
        checklist.setStatus(ChecklistStatus.APPROVED);
        itChecklistRepository.save(checklist);

        ExitRequest exitRequest = checklist.getExitRequest();
        exitRequest.setStatus(RequestStatus.SUCCESS);
        exitRequestRepository.save(exitRequest);
        log.info("IT clearance approved for checklistId: {}, exit request {} marked SUCCESS", checklistId, exitRequest.getRequestId());

        logAudit(exitRequest, itUser, ChecklistMessages.AUDIT_ACTION_APPROVED_IT, ChecklistMessages.AUDIT_COMMENT_APPROVED_IT);
        return mapToDto(checklist);
    }

    @Transactional
    public ItChecklistDto itReject(String itEmail, Long checklistId, ApprovalRequestDto approval) {
        log.info("IT rejecting checklistId: {} by user: {}", checklistId, itEmail);
        Employee itUser = employeeRepository.findByEmail(itEmail).get();
        ItChecklist checklist = itChecklistRepository.findById(checklistId)
                .orElseThrow(() -> new ResourceNotFoundException(ChecklistExceptionMessages.CHECKLIST_NOT_FOUND));

        if (checklist.getStatus() != ChecklistStatus.PENDING) {
            throw new InvalidStateTransitionException(ChecklistExceptionMessages.CHECKLIST_NOT_PENDING);
        }

        checklist.setStatus(ChecklistStatus.REJECTED);
        itChecklistRepository.save(checklist);

        ExitRequest exitRequest = checklist.getExitRequest();
        logAudit(exitRequest, itUser, ChecklistMessages.AUDIT_ACTION_REJECTED_IT, approval.getComments());

        // Create next iteration
        ItChecklist nextIteration = ItChecklist.builder()
                .exitRequest(exitRequest)
                .iteration(checklist.getIteration() + 1)
                .status(ChecklistStatus.PENDING)
                .build();
        itChecklistRepository.save(nextIteration);
        log.info("IT checklist rejected for checklistId: {}, new iteration {} created", checklistId, nextIteration.getIteration());

        return mapToDto(checklist);
    }

    private ItChecklistDto mapToDto(ItChecklist checklist) {
        return ItChecklistDto.builder()
                .checklistId(checklist.getChecklistId())
                .requestId(checklist.getExitRequest().getRequestId())
                .idCardReceived(checklist.isIdCardReceived())
                .accessCardReceived(checklist.isAccessCardReceived())
                .laptopReceived(checklist.isLaptopReceived())
                .documentPath(checklist.getDocumentPath())
                .status(checklist.getStatus())
                .iteration(checklist.getIteration())
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
        log.debug("Audit log saved: requestId={}, action={}, actorId={}", request.getRequestId(), action, actor.getEmpId());
    }
}
