package dev.aratax.example.model.vo;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
    int status,            // HTTP status 
    String message,        // human-readable summary
    Instant timestamp,     // when error was produced
    String path,           // request URI
    List<String> details   // field-level errors, optional
) {

    public static ApiErrorResponse of(
            int status,
            String message,
            String path,
            List<String> details
    ) {
        return new ApiErrorResponse(
            status,
            message,
            Instant.now(),
            path,
            details
        );
    }

}
