# Employee Exit Management Module - System Design & Implementation Plan

## Goal Description
Build a centralized Employee Exit Module using Spring Boot 3.5.x, Spring Security (JWT), and a relational database (MySQL or PostgreSQL) to handle the full offboarding lifecycle of an employee. This includes submission, L1/HR approvals, IT clearance, strict role-based access control, and enforcement of business rules (cooldowns, auto-rejection of stale requests).

## User Review Required
> [!IMPORTANT]
> 1. **File Uploads (Checklist):** Do not store files as BLOBs in the DB directly for performance reasons. Instead, we can implement local file system storage (or AWS S3) and save the `doc_url` or `file_path` in the `it_checklists` table. Please confirm if saving them on the local disk inside the workspace is acceptable for this implementation for now.
> 2. **Target Database:** Please confirm if you prefer **MySQL** or **PostgreSQL** so we configure the `pom.xml` and `application.yml` correctly.

## Architecture & Improvements
- **Layered N-tier Architecture:** Controller -> Service -> Repository -> Database.
- **Stateless Authentication:** JWT tokens passed in the `Authorization: Bearer <token>` header.
- **DTO validation:** Jakarta Validation API mapped directly in `Controller` methods (`@Valid`).
- **Global Exception Handling:** `@RestControllerAdvice` handling custom exceptions (e.g., `InvalidStateTransitionException`, `CooldownActiveException`).
- **Audit Trails:** Adding an explicit `AuditLog` entity to track state changes and actor IDs for enterprise traceability.
- **File Upload Strategy:** We will expose a `MultipartFile` endpoint for document uploads, saving the file to disk and associating it with the Checklist iteration.

## Proposed Changes

We will generate the standard Spring Boot layout inside your specified workspace: `d:\OneDrive - PeopleStrong\Desktop\ExitModule`.

### 1. Domain Layer (Entities & Enums)
Create JPA Entities mapped to the database schema.
- **Enums:** `Role` (EMPLOYEE, L1_MANAGER, HR, IT_OFFICE), `RequestStatus` (PENDING_L1, PENDING_HR, PENDING_IT_CLEARANCE, SUCCESS, REJECTED), `ChecklistStatus` (PENDING, APPROVED, REJECTED).
- **Employee (`employees`)**: `empId` (PK), `email` (Unique), `password`, `role`, self-referencing `l1Manager` & `hrManager` relationships. `LocalDate cooldownUntil`.
- **ExitRequest (`exit_requests`)**: `requestId` (PK), `employee` (ManyToOne), `dateOfLeaving`, `reason`, `status`.
- **ItChecklist (`it_checklists`)**: `checklistId` (PK), `exitRequest` (ManyToOne), validation booleans (`idCardReceived` etc), `documentUrl`, `status`, `iteration`.
- **AuditLog (`audit_logs`)**: tracks status transitions, actors, timestamps, and comments.

### 2. Repository Layer
Create Spring Data JPA Interfaces:
- `EmployeeRepository`, `ExitRequestRepository`, `ItChecklistRepository`, `AuditLogRepository`.

### 3. Service Layer (Business Logic)
- **AuthService**: User registration, password hashing (BCrypt), and JWT generation.
- **ExitRequestService**: 
  - `applyForExit()`: Checks if `employee.cooldownUntil` > current date.
  - `approveByL1()`, `rejectByL1()`: Evaluates requests, updates state to `REJECTED` and sets `employee.cooldownUntil = now() + 30 days` on rejection.
  - Transactions managed by `@Transactional`.
- **ItChecklistService**: Generates initial checklist upon HR approval `PENDING_IT_CLEARANCE`. Automatically creates N+1 iteration rows if IT rejects the prior checklist.
- **AutoRejectScheduler**: 
  - Uses `@Scheduled(cron = "0 0 0 * * ?")`.
  - Queries `exit_requests` in `PENDING_L1` or `PENDING_HR` states where `updated_at < now() - 7 days`.
  - Rejects the requests, updates employee cooldown, and commits an `AuditLog`.

### 4. Security Layer
- **JwtAuthenticationFilter**: Validates incoming tokens and populates the `SecurityContext`.
- **SecurityConfig**: Secures endpoints based on roles:
  - `/api/exit/l1/**` -> `hasRole('L1_MANAGER')`
  - `/api/exit/hr/**` -> `hasRole('HR')`
  - `/api/checklist/it/**` -> `hasRole('IT_OFFICE')`

### 5. API & Controllers Layer
Create REST controllers mapping to DTOs (e.g., `ExitRequestDto`, `ApprovalRequestDto`). We will avoid binding JPA Entities to the presentation layer. Use an `ApiResponse<T>` wrapper to return standardized `{ timestamp, status, error, data }` responses.

## Verification Plan

### Automated Tests
- Write fast unit tests for `ExitRequestService` to assert state machine logic (e.g. attempting to skip HR approval directly to IT clearance throws `InvalidStateTransitionException`).
- Write unit tests for the cooldown logic and next-iteration IT Checklist creation logic.

### Manual Verification
- We will build and start the application locally using `mvn spring-boot:run`.
- Provide a sequence of `curl` commands (or a Postman collection JSON artifact) to:
  1. Signup/Setup sample users with all 4 roles.
  2. Submits as EMPLOYEE -> check status is `PENDING_L1`.
  3. Approve as L1_MANAGER.
  4. Approve as HR -> Verify an IT Checklist is generated.
  5. Reject as IT_OFFICE -> Verify a new Checklist iteration is generated.
  6. Approve as IT_OFFICE -> Verify state transitions to `SUCCESS`.
