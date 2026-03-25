package com.PeopleStrong.ExitModule.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuditHistoryDto {
    private Long actorId;
    private String actorName;
    private String action;
    private String comments;
    private LocalDateTime timestamp;
}
