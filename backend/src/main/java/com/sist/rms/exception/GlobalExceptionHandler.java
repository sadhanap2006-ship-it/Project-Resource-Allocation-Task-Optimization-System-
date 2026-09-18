package com.sist.rms.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.ArrayList;
import java.util.List;

/**
 * One place that turns every exception into a friendly JSON message,
 * so the browser never receives a Java stack trace.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Unknown id -> 404 Not Found. */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex) {
        ApiError body = new ApiError(HttpStatus.NOT_FOUND.value(), "Not Found", List.of(ex.getMessage()));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    /** Broken business rule -> 400 Bad Request. */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiError> handleValidation(ValidationException ex) {
        ApiError body = new ApiError(HttpStatus.BAD_REQUEST.value(), "Validation Failed", ex.getMessages());
        return ResponseEntity.badRequest().body(body);
    }

    /** @Valid failure on a DTO field -> 400 Bad Request with every field message. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleBeanValidation(MethodArgumentNotValidException ex) {
        List<String> messages = new ArrayList<>();
        ex.getBindingResult().getFieldErrors()
          .forEach(fieldError -> messages.add(fieldError.getDefaultMessage()));
        if (messages.isEmpty()) {
            messages.add("Request is not valid");
        }
        ApiError body = new ApiError(HttpStatus.BAD_REQUEST.value(), "Validation Failed", messages);
        return ResponseEntity.badRequest().body(body);
    }

    /** Malformed JSON or a bad date like "2026-13-45" -> 400 Bad Request. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex) {
        ApiError body = new ApiError(HttpStatus.BAD_REQUEST.value(), "Validation Failed",
                List.of("Request body is not valid JSON, or a date is not in yyyy-MM-dd format"));
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * A page or file that does not exist -> 404, not 500.
     * Without this, the catch-all below would turn a missing favicon or a
     * mistyped page address into a server error.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleMissingStaticResource(NoResourceFoundException ex) {
        ApiError body = new ApiError(HttpStatus.NOT_FOUND.value(), "Not Found",
                List.of("No page or file found at " + ex.getResourcePath()));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    /** Anything unexpected -> 500, but still as clean JSON. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleEverythingElse(Exception ex) {
        ApiError body = new ApiError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Server Error",
                List.of(ex.getMessage() == null ? "Unexpected server error" : ex.getMessage()));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
