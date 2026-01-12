package com.lre.gitlabintegration.dto.apierror;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import java.time.Instant;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
    public static ApiErrorResponse of(HttpStatus status, String message, String path) {
        return new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path
        );
    }

    public static ApiErrorResponse of(HttpStatusCode status, String message, String path) {
        return of(HttpStatus.valueOf(status.value()), message, path);
    }
}
