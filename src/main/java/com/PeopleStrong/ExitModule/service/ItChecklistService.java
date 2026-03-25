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
import lombok.RequiredArgsConstructor;
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
public class ItChecklistService {

    private final ItChecklistRepository itChecklistRepository;
    private final ExitRequestRepository exitRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final AuditLogRepository auditLogRepository;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    @Transactional
    public ItChecklistDto uploadDocument(String employeeEmail, Long requestId, MultipartFile file) {
        Employee employee = employeeRepository.findByEmail(employeeEmail).get();
        ExitRequest exitRequest = exitRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Exit Request not found"));

        if (!exitRequest.getEmployee().getEmpId().equals(employee.getEmpId())) {
            throw new InvalidStateTransitionException("Not your request");
        }

        ItChecklist activeChecklist = itChecklistRepository.findByExitRequest_RequestId(requestId).stream()
                .filter(c -> c.getStatus() == ChecklistStatus.PENDING)
                .findFirst()
                .orElseThrow(() -> new InvalidStateTransitionException("No pending checklist found for upload"));

        // Save file
        try {
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();

            String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path path = Paths.get(uploadDir, filename);
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
            activeChecklist.setDocumentPath(path.toString());
            itChecklistRepository.save(activeChecklist);

            return mapToDto(activeChecklist);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    public List<ItChecklistDto> getPendingChecklists() {
        return itChecklistRepository.findByStatus(ChecklistStatus.PENDING)
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional
    public ItChecklistDto itApprove(String itEmail, Long checklistId) {
        Employee itUser = employeeRepository.findByEmail(itEmail).get();
        ItChecklist checklist = itChecklistRepository.findById(checklistId)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist not found"));

        if (checklist.getStatus() != ChecklistStatus.PENDING) {
            throw new InvalidStateTransitionException("Checklist is not pending");
        }

        checklist.setIdCardReceived(true);
        checklist.setAccessCardReceived(true);
        checklist.setLaptopReceived(true);
        checklist.setStatus(ChecklistStatus.APPROVED);
        itChecklistRepository.save(checklist);

        ExitRequest exitRequest = checklist.getExitRequest();
        exitRequest.setStatus(RequestStatus.SUCCESS);
        exitRequestRepository.save(exitRequest);

        logAudit(exitRequest, itUser, "APPROVED_IT", "IT completed clearance. Exit successful.");
        return mapToDto(checklist);
    }

    @Transactional
    public ItChecklistDto itReject(String itEmail, Long checklistId, ApprovalRequestDto approval) {
        Employee itUser = employeeRepository.findByEmail(itEmail).get();
        ItChecklist checklist = itChecklistRepository.findById(checklistId)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist not found"));

        if (checklist.getStatus() != ChecklistStatus.PENDING) {
            throw new InvalidStateTransitionException("Checklist is not pending");
        }

        checklist.setStatus(ChecklistStatus.REJECTED);
        itChecklistRepository.save(checklist);

        ExitRequest exitRequest = checklist.getExitRequest();
        logAudit(exitRequest, itUser, "REJECTED_IT", approval.getComments());

        // Create next iteration
        ItChecklist nextIteration = ItChecklist.builder()
                .exitRequest(exitRequest)
                .iteration(checklist.getIteration() + 1)
                .status(ChecklistStatus.PENDING)
                .build();
        itChecklistRepository.save(nextIteration);

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
    }
}
