package edu.sist.prs.exception;

import org.springframework.http.HttpStatus;
import java.util.Map;

public class BadRequestException extends ApiException {
    public BadRequestException(String errorCode, String message, Map<String, Object> details) {
        super(HttpStatus.BAD_REQUEST, errorCode, message, details);
    }

    public BadRequestException(String errorCode, String message) {
        super(HttpStatus.BAD_REQUEST, errorCode, message);
    }

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", message);
    }
}
