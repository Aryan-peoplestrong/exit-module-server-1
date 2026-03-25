# Employees Exit Management Module - Backend Documentation

## 1. Overview
The Exit Management Module is a Spring Boot application designed to handle the offboarding process of employees. It features role-based access control, JWT authentication, hierarchical approvals (L1 Manager, HR), and an IT clearance checklist system.

## 2. Roles and Authorities
- **Employee**: Can submit an exit request and view their own requests.
- **L1 Manager**: Approves or rejects the exit requests of their reportees.
- **HR Manager**: Approves or rejects the exit requests after L1 approval.
- **IT Admin**: Manages IT checklists (Asset recovery).

## 3. Database Schema (Entities)
### a) Employee (`employees`)
- `empId` (PK), `name`, `email` (Unique), `password`, `role` (Enum)
- `l1Manager_id` (FK -> Employee)
- `hrManager_id` (FK -> Employee)
- `cooldownUntil` (Date)
### b) ExitRequest (`exit_requests`)
- `requestId` (PK), `emp_id` (FK -> Employee), `dateOfLeaving`, `reason`
- `status` (Enum: PENDING_L1, PENDING_HR, PENDING_IT_CLEARANCE, SUCCESS, REJECTED)
- `rejectionReason`, `createdAt`, `lastUpdated`
### c) ItChecklist (`it_checklists`)
- `checklistId` (PK), `request_id` (FK -> ExitRequest)
- `idCardReceived` (Boolean), `accessCardReceived` (Boolean), `laptopReceived` (Boolean)
- `documentPath` (File path), `status` (Enum: PENDING, APPROVED, REJECTED)
- `iteration` (Integer - tracks how many times it was rejected and re-created)
### d) AuditLog (`audit_logs`)
- `auditId` (PK), `request_id` (FK -> ExitRequest), `actor_id` (FK -> Employee)
- `action` (String), `comments`, `timestamp`

## 4. Workflows & Business Logic
### Exit Request Flow
1. **Submission**: Employee applies for an exit request. System checks if there is any active cooldown period or an existing open request. Status: `PENDING_L1`.
2. **L1 Manager Approval**: L1 manager reviews. If approved, status -> `PENDING_HR`. If rejected, status -> `REJECTED`, rejection reason saved, and an active cooldown period (30 days) is applied to the employee.
3. **HR Manager Approval**: HR reviews. If approved, status -> `PENDING_IT_CLEARANCE`. An initial IT Checklist (Iteration 1) is generated with status `PENDING`. If rejected, status -> `REJECTED`, and 30 days cooldown applied.
4. **IT Clearance**: IT department checks if assets are returned. Employee can upload clearance documents. IT manager approves or rejects.
   - If IT approves: All assets are marked received. ExitRequest status -> `SUCCESS`.
   - If IT rejects: IT Checklist status -> `REJECTED`. A new IT Checklist (Iteration = current + 1) is created with status `PENDING`.

*All the above steps are recorded in the `audit_logs` table.*

## 5. API Routes

### Authentication (`/api/auth`)
- `POST /signup`: Register a new user (Requires name, email, password, role, L1 Manager ID, HR Manager ID).
- `POST /login`: Generate JWT token for user.

### Exit Requests (`/api/exit`)
- `POST /apply`: Submit a new exit request.
- `GET /my-request`: Get requests created by the logged-in employee.
- `GET /l1/requests`: Get all requests pending for the logged-in L1 Manager.
- `POST /l1/approve/{id}`: Approve request by L1 Manager.
- `POST /l1/reject/{id}`: Reject request by L1 Manager (Requires comments).
- `GET /hr/requests`: Get all requests pending for the logged-in HR Manager.
- `POST /hr/approve/{id}`: Approve request by HR Manager.
- `POST /hr/reject/{id}`: Reject request by HR Manager (Requires comments).

### IT Checklist (`/api/checklist`)
- `POST /upload-docs/{requestId}`: Upload a clearance document for a specific request.
- `GET /pending/it`: Get all pending IT checklists.
- `POST /it/approve/{checklistId}`: IT Admin approves the checklist (Marks assets returned).
- `POST /it/reject/{checklistId}`: IT Admin rejects the checklist (Creates new iteration).
