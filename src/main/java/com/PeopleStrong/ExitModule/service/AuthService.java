package com.PeopleStrong.ExitModule.service;

import com.PeopleStrong.ExitModule.model.Employee;
import com.PeopleStrong.ExitModule.dto.AuthRequestDto;
import com.PeopleStrong.ExitModule.dto.AuthResponseDto;
import com.PeopleStrong.ExitModule.dto.SignupRequestDto;
import com.PeopleStrong.ExitModule.repository.EmployeeRepository;
import com.PeopleStrong.ExitModule.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponseDto signup(SignupRequestDto request) {
        if (employeeRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email is already in use");
        }

        Employee l1Manager = null;
        if (request.getL1ManagerId() != null) {
            l1Manager = employeeRepository.findById(request.getL1ManagerId())
                    .orElseThrow(() -> new RuntimeException("L1 Manager not found"));
        }

        Employee hrManager = null;
        if (request.getHrManagerId() != null) {
            hrManager = employeeRepository.findById(request.getHrManagerId())
                    .orElseThrow(() -> new RuntimeException("HR Manager not found"));
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

        String jwt = jwtUtils.generateToken(employee);

        return AuthResponseDto.builder()
                .token(jwt)
                .empId(employee.getEmpId())
                .name(employee.getName())
                .email(employee.getEmail())
                .role(employee.getRole())
                .build();
    }

    public AuthResponseDto login(AuthRequestDto request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        Employee employee = (Employee) authentication.getPrincipal();
        String jwt = jwtUtils.generateToken(employee);

        return AuthResponseDto.builder()
                .token(jwt)
                .empId(employee.getEmpId())
                .name(employee.getName())
                .email(employee.getEmail())
                .role(employee.getRole())
                .build();
    }
}
