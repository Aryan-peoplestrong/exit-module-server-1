package com.PeopleStrong.ExitModule.common;

public final class SchedulerMessages {

    private SchedulerMessages() {}

    public static final String AUTO_REJECT_ACTION  = "AUTO_REJECTED";
    public static final String AUTO_REJECT_REASON  = "System Auto-Reject due to 7 days of inactivity.";
    public static final String AUTO_REJECT_COMMENT = "System auto-rejected request due to inactivity";

    // ── Log messages ─────────────────────────────────────────────────────────
    public static final String LOG_SCHEDULER_START        = "Running auto-reject scheduler for stale requests...";
    public static final String LOG_NO_STALE_REQUESTS      = "No stale requests found.";
    public static final String LOG_AUTO_REJECTING_REQUEST = "Auto-rejecting request ID {}";
    public static final String LOG_REJECTION_COMPLETE     = "Successfully rejected {} stale requests.";
}
