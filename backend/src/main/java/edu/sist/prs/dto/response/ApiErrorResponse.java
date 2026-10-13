package edu.sist.prs.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record ApiErrorResponse(
    String status,
    String errorCode,
    String message,
    Map<String, Object> details,
    List<FieldErrorItem> fieldErrors,
    LocalDateTime timestamp
) {
    public static ApiErrorResponse of(String errorCode, String message, Map<String, Object> details, List<FieldErrorItem> fieldErrors) {
        return new ApiErrorResponse(
            "ERROR",
            errorCode,
            message,
            details != null ? details : Map.of(),
            fieldErrors != null ? fieldErrors : List.of(),
            LocalDateTime.now()
        );
    }

    public static ApiErrorResponse of(String errorCode, String message) {
        return of(errorCode, message, Map.of(), List.of());
    }
}
