package com.PeopleStrong.ExitModule.common;

/**
 * Exception message constants for ItChecklistService.
 */
public final class ChecklistExceptionMessages {

    private ChecklistExceptionMessages() {}

    // ResourceNotFoundException
    public static final String EXIT_REQUEST_NOT_FOUND             = "Exit Request not found";
    public static final String CHECKLIST_NOT_FOUND                = "Checklist not found";

    // InvalidStateTransitionException
    public static final String NOT_YOUR_REQUEST                   = "Not your request";
    public static final String NO_PENDING_CHECKLIST_FOR_UPLOAD    = "No pending checklist found for upload";
    public static final String CHECKLIST_NOT_PENDING              = "Checklist is not pending";

    // RuntimeException
    public static final String FILE_STORE_FAILED                  = "Failed to store file";
}
