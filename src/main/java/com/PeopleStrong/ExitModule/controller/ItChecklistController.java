package com.PeopleStrong.ExitModule.controller;

import com.PeopleStrong.ExitModule.common.ChecklistMessages;
import com.PeopleStrong.ExitModule.common.GeneralMessages;
import com.PeopleStrong.ExitModule.dto.ApiResponse;
import com.PeopleStrong.ExitModule.dto.ApprovalRequestDto;
import com.PeopleStrong.ExitModule.dto.ItChecklistDto;
import com.PeopleStrong.ExitModule.exception.InvalidStateTransitionException;
import com.PeopleStrong.ExitModule.exception.ResourceNotFoundException;
import com.PeopleStrong.ExitModule.service.ItChecklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/checklist")
@RequiredArgsConstructor
public class ItChecklistController {

    private final ItChecklistService itChecklistService;

    @PostMapping("/upload-docs/{requestId}")
    public ResponseEntity<ApiResponse<ItChecklistDto>> uploadDocs(
            @PathVariable Long requestId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        try {
            return okResponse(
                    itChecklistService.uploadDocument(authentication.getName(), requestId, file),
                    ChecklistMessages.DOCUMENT_UPLOADED);
        } catch (ResourceNotFoundException e) {
            return errorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (InvalidStateTransitionException | IllegalArgumentException e) {
            return errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, GeneralMessages.UNEXPECTED_ERROR);
        }
    }

    @GetMapping("/pending/it")
    public ResponseEntity<ApiResponse<List<ItChecklistDto>>> getPendingChecklists() {
        try {
            return okResponse(itChecklistService.getPendingChecklists(),
                    ChecklistMessages.FETCHED_PENDING_CHECKLISTS);
        } catch (Exception e) {
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, GeneralMessages.UNEXPECTED_ERROR);
        }
    }

    @PostMapping("/it/approve/{checklistId}")
    public ResponseEntity<ApiResponse<ItChecklistDto>> itApprove(
            @PathVariable Long checklistId, Authentication authentication) {
        try {
            return okResponse(itChecklistService.itApprove(authentication.getName(), checklistId),
                    ChecklistMessages.CHECKLIST_APPROVED);
        } catch (ResourceNotFoundException e) {
            return errorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (InvalidStateTransitionException e) {
            return errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, GeneralMessages.UNEXPECTED_ERROR);
        }
    }

    @PostMapping("/it/reject/{checklistId}")
    public ResponseEntity<ApiResponse<ItChecklistDto>> itReject(
            @PathVariable Long checklistId,
            @RequestBody ApprovalRequestDto approval,
            Authentication authentication) {
        try {
            return okResponse(itChecklistService.itReject(authentication.getName(), checklistId, approval),
                    ChecklistMessages.CHECKLIST_REJECTED);
        } catch (ResourceNotFoundException e) {
            return errorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (InvalidStateTransitionException | IllegalArgumentException e) {
            return errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, GeneralMessages.UNEXPECTED_ERROR);
        }
    }

    private <T> ResponseEntity<ApiResponse<T>> okResponse(T data, String message) {
        return ResponseEntity.ok(ApiResponse.<T>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .message(message)
                .data(data)
                .build());
    }

    private <T> ResponseEntity<ApiResponse<T>> errorResponse(HttpStatus status, String errorMsg) {
        return ResponseEntity.status(status.value()).body(ApiResponse.<T>builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .message(GeneralMessages.ERROR)
                .error(errorMsg)
                .build());
    }
}
