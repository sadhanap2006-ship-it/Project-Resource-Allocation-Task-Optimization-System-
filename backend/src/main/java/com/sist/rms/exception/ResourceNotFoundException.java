package com.sist.rms.exception;

/** Thrown when an id does not exist. Turned into HTTP 404 by the global handler. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    /** Convenience: new ResourceNotFoundException("Project", 9) -> "Project not found with id 9". */
    public ResourceNotFoundException(String entityName, Object id) {
        super(entityName + " not found with id " + id);
    }
}
