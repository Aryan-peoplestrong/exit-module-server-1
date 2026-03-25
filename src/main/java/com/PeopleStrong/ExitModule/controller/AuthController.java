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
import org.springframework.security.core.AuthenticationException;
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
        try {
            AuthResponseDto response = authService.signup(request);
            return okResponse(response, "User registered successfully");
        } catch (RuntimeException e) {
            return errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDto>> login(@Valid @RequestBody AuthRequestDto request) {
        try {
            AuthResponseDto response = authService.login(request);
            return okResponse(response, "Login successful");
        } catch (AuthenticationException e) {
            return errorResponse(HttpStatus.UNAUTHORIZED, "Invalid credentials or unauthorized access");
        } catch (RuntimeException e) {
            return errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred: " + e.getMessage());
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
