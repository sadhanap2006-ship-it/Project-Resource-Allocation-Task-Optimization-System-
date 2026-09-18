package com.sist.rms.exception;

import java.util.ArrayList;
import java.util.List;

/**
 * Thrown by the service layer when a business rule is broken
 * (for example "End date cannot be before start date").
 * Turned into HTTP 400 by the global handler.
 */
public class ValidationException extends RuntimeException {

    private final List<String> messages = new ArrayList<>();

    public ValidationException(String message) {
        super(message);
        this.messages.add(message);
    }

    public ValidationException(List<String> messages) {
        super(String.join("; ", messages));
        this.messages.addAll(messages);
    }

    public List<String> getMessages() {
        return messages;
    }
}
