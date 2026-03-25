package com.PeopleStrong.ExitModule.exception;

public class CooldownActiveException extends RuntimeException {
    public CooldownActiveException(String message) {
        super(message);
    }
}
