package com.PeopleStrong.ExitModule.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateExitRequestDto {
    @NotNull(message = "Date of leaving is required")
    @Future(message = "Date of leaving must be in the future")
    private LocalDate dateOfLeaving;

    @NotBlank(message = "Reason is required")
    private String reason;
}
