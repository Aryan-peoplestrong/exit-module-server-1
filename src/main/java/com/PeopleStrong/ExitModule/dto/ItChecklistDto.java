package com.PeopleStrong.ExitModule.dto;

import com.PeopleStrong.ExitModule.model.enums.ChecklistStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ItChecklistDto {
    private Long checklistId;
    private Long requestId;
    private boolean idCardReceived;
    private boolean accessCardReceived;
    private boolean laptopReceived;
    private String documentPath;
    private ChecklistStatus status;
    private int iteration;
}
