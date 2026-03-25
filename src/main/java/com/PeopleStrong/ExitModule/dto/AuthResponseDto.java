package com.PeopleStrong.ExitModule.dto;

import com.PeopleStrong.ExitModule.model.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDto {
    private String token;
    private Long empId;
    private String name;
    private String email;
    private Role role;
}
