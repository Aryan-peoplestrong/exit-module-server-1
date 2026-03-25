package com.PeopleStrong.ExitModule.controller;

import com.PeopleStrong.ExitModule.dto.ApiResponse;
import com.PeopleStrong.ExitModule.dto.ApprovalRequestDto;
import com.PeopleStrong.ExitModule.dto.ItChecklistDto;
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
        return okResponse(itChecklistService.uploadDocument(authentication.getName(), requestId, file), "Document uploaded");
    }

    @GetMapping("/pending/it")
    public ResponseEntity<ApiResponse<List<ItChecklistDto>>> getPendingChecklists() {
        return okResponse(itChecklistService.getPendingChecklists(), "Fetched pending IT checklists");
    }

    @PostMapping("/it/approve/{checklistId}")
    public ResponseEntity<ApiResponse<ItChecklistDto>> itApprove(@PathVariable Long checklistId, Authentication authentication) {
        return okResponse(itChecklistService.itApprove(authentication.getName(), checklistId), "Checklist approved");
    }

    @PostMapping("/it/reject/{checklistId}")
    public ResponseEntity<ApiResponse<ItChecklistDto>> itReject(@PathVariable Long checklistId, @RequestBody ApprovalRequestDto approval, Authentication authentication) {
        return okResponse(itChecklistService.itReject(authentication.getName(), checklistId, approval), "Checklist rejected, new iteration created");
    }

    private <T> ResponseEntity<ApiResponse<T>> okResponse(T data, String message) {
        return ResponseEntity.ok(ApiResponse.<T>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .message(message)
                .data(data)
                .build());
    }
}
