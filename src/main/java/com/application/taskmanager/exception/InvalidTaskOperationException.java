package com.application.taskmanager.exception;

public class InvalidTaskOperationException extends RuntimeException {
    public InvalidTaskOperationException(String message) {
        super(message);
    }
}
