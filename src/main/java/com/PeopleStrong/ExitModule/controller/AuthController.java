package com.PeopleStrong.ExitModule.controller;

import com.PeopleStrong.ExitModule.dto.ApiResponse;
import com.PeopleStrong.ExitModule.dto.AuthRequestDto;
import com.PeopleStrong.ExitModule.dto.AuthResponseDto;
import com.PeopleStrong.ExitModule.dto.SignupRequestDto;
import com.PeopleStrong.ExitModule.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthResponseDto>> signup(@Valid @RequestBody SignupRequestDto request) {
        AuthResponseDto response = authService.signup(request);
        return ResponseEntity.ok(ApiResponse.<AuthResponseDto>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .message("User registered successfully")
                .data(response)
                .build());
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDto>> login(@Valid @RequestBody AuthRequestDto request) {
        AuthResponseDto response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.<AuthResponseDto>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .message("Login successful")
                .data(response)
                .build());
    }
}
