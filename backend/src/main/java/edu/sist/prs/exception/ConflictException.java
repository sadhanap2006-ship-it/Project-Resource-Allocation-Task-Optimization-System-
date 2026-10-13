package edu.sist.prs.exception;

import org.springframework.http.HttpStatus;
import java.util.Map;

public class ConflictException extends ApiException {
    public ConflictException(String errorCode, String message, Map<String, Object> details) {
        super(HttpStatus.CONFLICT, errorCode, message, details);
    }

    public ConflictException(String errorCode, String message) {
        super(HttpStatus.CONFLICT, errorCode, message);
    }
}
