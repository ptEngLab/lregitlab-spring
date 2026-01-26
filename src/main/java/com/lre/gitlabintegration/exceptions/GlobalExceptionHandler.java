package com.lre.gitlabintegration.exceptions;

import com.lre.gitlabintegration.dto.apierror.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<@NonNull ApiErrorResponse> build(HttpStatusCode status, String message, HttpServletRequest request) {
        ApiErrorResponse body = ApiErrorResponse.of(status, message, request.getRequestURI());
        return ResponseEntity.status(status.value()).body(body);
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<@NonNull ApiErrorResponse> handleAuth(AuthException ex, HttpServletRequest request) {
        return build(ex.getStatus(), ex.getMessage(), request);
    }

    @ExceptionHandler(ServerErrorException.class)
    public ResponseEntity<@NonNull ApiErrorResponse> handleServerError(ServerErrorException ex, HttpServletRequest request) {
        return build(ex.getStatus(), ex.getMessage(), request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<@NonNull ApiErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return build(ex.getStatus(), ex.getMessage(), request);
    }

    @ExceptionHandler(ClientErrorException.class)
    public ResponseEntity<@NonNull ApiErrorResponse> handleClientError(ClientErrorException ex, HttpServletRequest request) {
        return build(ex.getStatus(), ex.getMessage(), request);
    }

    @ExceptionHandler(LreException.class)
    public ResponseEntity<@NonNull ApiErrorResponse> handleLreException(LreException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ApiErrorResponse body = ApiErrorResponse.of(status, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<@NonNull ApiErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error", ex);
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ApiErrorResponse body = ApiErrorResponse.of(status, "An unexpected error occurred", request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
