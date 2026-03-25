package com.PeopleStrong.ExitModule.controller;

import com.PeopleStrong.ExitModule.dto.ApiResponse;
import com.PeopleStrong.ExitModule.dto.ApprovalRequestDto;
import com.PeopleStrong.ExitModule.dto.CreateExitRequestDto;
import com.PeopleStrong.ExitModule.dto.ExitRequestDto;
import com.PeopleStrong.ExitModule.dto.AuditHistoryDto;
import com.PeopleStrong.ExitModule.exception.CooldownActiveException;
import com.PeopleStrong.ExitModule.exception.InvalidStateTransitionException;
import com.PeopleStrong.ExitModule.exception.ResourceNotFoundException;
import com.PeopleStrong.ExitModule.service.ExitRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/exit")
@RequiredArgsConstructor
public class ExitRequestController {

    private final ExitRequestService exitRequestService;

    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<ExitRequestDto>> applyForExit(@Valid @RequestBody CreateExitRequestDto request,
            Authentication authentication) {
        try {
            ExitRequestDto response = exitRequestService.applyForExit(authentication.getName(), request);
            return okResponse(response, "Exit request submitted successfully");
        } catch (ResourceNotFoundException e) {
            return errorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (CooldownActiveException | InvalidStateTransitionException | IllegalArgumentException e) {
            return errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        }
    }

    @GetMapping("/my-request")
    public ResponseEntity<ApiResponse<List<ExitRequestDto>>> getMyRequests(Authentication authentication) {
        try {
            return okResponse(exitRequestService.getMyRequests(authentication.getName()), "Fetched your requests");
        } catch (ResourceNotFoundException e) {
            return errorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        }
    }

    @GetMapping("/l1/requests")
    public ResponseEntity<ApiResponse<List<ExitRequestDto>>> getL1Requests(Authentication authentication) {
        try {
            return okResponse(exitRequestService.getRequestsForL1(authentication.getName()),
                    "Fetched L1 reportee requests");
        } catch (ResourceNotFoundException e) {
            return errorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        }
    }

    @PostMapping("/l1/approve/{id}")
    public ResponseEntity<ApiResponse<ExitRequestDto>> l1Approve(@PathVariable Long id, Authentication authentication) {
        try {
            return okResponse(exitRequestService.l1Approve(authentication.getName(), id), "Request approved by L1");
        } catch (ResourceNotFoundException e) {
            return errorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (InvalidStateTransitionException e) {
            return errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        }
    }

    @PostMapping("/l1/reject/{id}")
    public ResponseEntity<ApiResponse<ExitRequestDto>> l1Reject(@PathVariable Long id,
            @RequestBody ApprovalRequestDto approval, Authentication authentication) {
        try {
            return okResponse(exitRequestService.l1Reject(authentication.getName(), id, approval),
                    "Request rejected by L1");
        } catch (ResourceNotFoundException e) {
            return errorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (InvalidStateTransitionException | IllegalArgumentException e) {
            return errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        }
    }

    @GetMapping("/hr/requests")
    public ResponseEntity<ApiResponse<List<ExitRequestDto>>> getHrRequests(Authentication authentication) {
        try {
            return okResponse(exitRequestService.getRequestsForHr(authentication.getName()),
                    "Fetched HR reportee requests");
        } catch (ResourceNotFoundException e) {
            return errorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        }
    }

    @PostMapping("/hr/approve/{id}")
    public ResponseEntity<ApiResponse<ExitRequestDto>> hrApprove(@PathVariable Long id, Authentication authentication) {
        try {
            return okResponse(exitRequestService.hrApprove(authentication.getName(), id), "Request approved by HR");
        } catch (ResourceNotFoundException e) {
            return errorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (InvalidStateTransitionException e) {
            return errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        }
    }

    @PostMapping("/hr/reject/{id}")
    public ResponseEntity<ApiResponse<ExitRequestDto>> hrReject(@PathVariable Long id,
            @RequestBody ApprovalRequestDto approval, Authentication authentication) {
        try {
            return okResponse(exitRequestService.hrReject(authentication.getName(), id, approval),
                    "Request rejected by HR");
        } catch (ResourceNotFoundException e) {
            return errorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (InvalidStateTransitionException | IllegalArgumentException e) {
            return errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        }
    }

    @GetMapping("/{id}/audit-history")
    public ResponseEntity<ApiResponse<List<AuditHistoryDto>>> getAuditHistoryById(@PathVariable Long id) {
        try {
            return okResponse(exitRequestService.getAuditHistoryById(id), "Fetched audit history");
        } catch (ResourceNotFoundException e) {
            return errorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
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
                .message("Error")
                .error(errorMsg)
                .build());
    }
}
