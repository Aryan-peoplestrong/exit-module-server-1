package com.PeopleStrong.ExitModule.dto;

import com.PeopleStrong.ExitModule.model.enums.RequestStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class ExitRequestDto {
    private Long requestId;
    private Long empId;
    private String empName;
    private LocalDate dateOfLeaving;
    private String reason;
    private RequestStatus status;
    private String rejectionReason;
}
