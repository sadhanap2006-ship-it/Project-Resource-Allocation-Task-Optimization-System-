package edu.sist.prs.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ApiException {
    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }

    public ResourceNotFoundException(String resourceType, Object id) {
        super(HttpStatus.NOT_FOUND, "NOT_FOUND", resourceType + " with id '" + id + "' not found");
    }
}
