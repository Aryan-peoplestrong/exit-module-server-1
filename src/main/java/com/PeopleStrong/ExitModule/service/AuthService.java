package com.PeopleStrong.ExitModule.service;

import com.PeopleStrong.ExitModule.model.Employee;
import com.PeopleStrong.ExitModule.dto.AuthRequestDto;
import com.PeopleStrong.ExitModule.dto.AuthResponseDto;
import com.PeopleStrong.ExitModule.dto.SignupRequestDto;
import com.PeopleStrong.ExitModule.common.AuthExceptionMessages;
import com.PeopleStrong.ExitModule.repository.EmployeeRepository;
import com.PeopleStrong.ExitModule.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponseDto signup(SignupRequestDto request) {
        log.info("Processing signup for email: {}", request.getEmail());

        if (employeeRepository.findByEmail(request.getEmail()).isPresent()) {
            log.warn("Signup failed - email already in use: {}", request.getEmail());
            throw new RuntimeException(AuthExceptionMessages.EMAIL_ALREADY_IN_USE);
        }

        Employee l1Manager = null;
        if (request.getL1ManagerId() != null) {
            log.debug("Looking up L1 manager with id: {}", request.getL1ManagerId());
            l1Manager = employeeRepository.findById(request.getL1ManagerId())
                    .orElseThrow(() -> new RuntimeException(AuthExceptionMessages.L1_MANAGER_NOT_FOUND));
        }

        Employee hrManager = null;
        if (request.getHrManagerId() != null) {
            log.debug("Looking up HR manager with id: {}", request.getHrManagerId());
            hrManager = employeeRepository.findById(request.getHrManagerId())
                    .orElseThrow(() -> new RuntimeException(AuthExceptionMessages.HR_MANAGER_NOT_FOUND));
        }

        Employee employee = Employee.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .l1Manager(l1Manager)
                .hrManager(hrManager)
                .build();

        employeeRepository.save(employee);
        log.info("Employee created successfully with empId: {}", employee.getEmpId());

        String jwt = jwtUtils.generateToken(employee);
        log.debug("JWT token generated for employee: {}", employee.getEmpId());

        return AuthResponseDto.builder()
                .token(jwt)
                .empId(employee.getEmpId())
                .name(employee.getName())
                .email(employee.getEmail())
                .role(employee.getRole())
                .build();
    }

    public AuthResponseDto login(AuthRequestDto request) {
        log.info("Processing login for email: {}", request.getEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        Employee employee = (Employee) authentication.getPrincipal();
        String jwt = jwtUtils.generateToken(employee);
        log.info("Login successful for empId: {}", employee.getEmpId());

        return AuthResponseDto.builder()
                .token(jwt)
                .empId(employee.getEmpId())
                .name(employee.getName())
                .email(employee.getEmail())
                .role(employee.getRole())
                .build();
    }
}
