package com.PeopleStrong.ExitModule.common;

public final class ChecklistMessages {

    private ChecklistMessages() {}

    // Controller response messages
    public static final String DOCUMENT_UPLOADED           = "Document uploaded successfully";
    public static final String FETCHED_PENDING_CHECKLISTS  = "Fetched pending IT checklists";
    public static final String CHECKLIST_APPROVED          = "Checklist approved";
    public static final String CHECKLIST_REJECTED          = "Checklist rejected, new iteration created";

    // Audit log action codes / comments (used in service)
    public static final String AUDIT_ACTION_APPROVED_IT    = "APPROVED_IT";
    public static final String AUDIT_COMMENT_APPROVED_IT   = "IT completed clearance. Exit successful.";
    public static final String AUDIT_ACTION_REJECTED_IT    = "REJECTED_IT";
}
