package com.PeopleStrong.ExitModule.common;

/**
 * Exception and audit-log message constants for ExitRequestService.
 */
public final class ExitRequestExceptionMessages {

    private ExitRequestExceptionMessages() {}

    // ResourceNotFoundException
    public static final String EMPLOYEE_NOT_FOUND     = "Employee not found";
    public static final String MANAGER_NOT_FOUND      = "Manager not found";
    public static final String HR_NOT_FOUND           = "HR not found";
    public static final String EXIT_REQUEST_NOT_FOUND = "Exit Request not found";

    // CooldownActiveException (prefix – caller appends the date)
    public static final String COOLDOWN_ACTIVE_PREFIX = "Cannot submit request. Cooldown active until ";

    // InvalidStateTransitionException
    public static final String ACTIVE_REQUEST_EXISTS  = "An active exit request already exists.";
    public static final String NOT_IN_PENDING_L1      = "Request is not in PENDING_L1 state";
    public static final String NOT_IN_PENDING_HR      = "Request is not in PENDING_HR state";
    public static final String NOT_L1_MANAGER         = "You are not the L1 Manager for this employee.";
    public static final String NOT_HR_MANAGER         = "You are not the HR Manager for this employee.";

    // IllegalArgumentException
    public static final String REJECTION_COMMENTS_MANDATORY = "Rejection comments are mandatory";

    // Audit-log action codes
    public static final String AUDIT_ACTION_SUBMITTED  = "SUBMITTED";
    public static final String AUDIT_ACTION_APPROVED_L1 = "APPROVED_L1";
    public static final String AUDIT_ACTION_REJECTED_L1 = "REJECTED_L1";
    public static final String AUDIT_ACTION_APPROVED_HR = "APPROVED_HR";
    public static final String AUDIT_ACTION_REJECTED_HR = "REJECTED_HR";

    // Audit-log comments
    public static final String AUDIT_COMMENT_SUBMITTED    = "Employee submitted exit request";
    public static final String AUDIT_COMMENT_APPROVED_L1  = "L1 Manager approved";
    public static final String AUDIT_COMMENT_APPROVED_HR  = "HR Manager approved. Pending IT checklist.";

    // Misc
    public static final String SYSTEM_ACTOR_NAME = "System";
}
