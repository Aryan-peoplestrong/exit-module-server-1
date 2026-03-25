package com.PeopleStrong.ExitModule.controller;

import com.PeopleStrong.ExitModule.common.AuthMessages;
import com.PeopleStrong.ExitModule.common.GeneralMessages;
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
            return okResponse(response, AuthMessages.USER_REGISTERED_SUCCESSFULLY);
        } catch (RuntimeException e) {
            return errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, GeneralMessages.UNEXPECTED_ERROR);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDto>> login(@Valid @RequestBody AuthRequestDto request) {
        try {
            AuthResponseDto response = authService.login(request);
            return okResponse(response, AuthMessages.LOGIN_SUCCESSFUL);
        } catch (AuthenticationException e) {
            return errorResponse(HttpStatus.UNAUTHORIZED, AuthMessages.INVALID_CREDENTIALS);
        } catch (RuntimeException e) {
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
