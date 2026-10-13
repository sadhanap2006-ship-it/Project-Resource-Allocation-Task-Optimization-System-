package edu.sist.prs.exception;

import org.springframework.http.HttpStatus;
import java.util.Map;

public class ApiException extends RuntimeException {
    private final HttpStatus httpStatus;
    private final String errorCode;
    private final Map<String, Object> details;

    public ApiException(HttpStatus httpStatus, String errorCode, String message, Map<String, Object> details) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.details = details != null ? details : Map.of();
    }

    public ApiException(HttpStatus httpStatus, String errorCode, String message) {
        this(httpStatus, errorCode, message, Map.of());
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
