package com.lre.gitlabintegration.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lre.gitlabintegration.dto.apierror.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

public record JsonAccessDeniedHandler(ObjectMapper objectMapper)
        implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {

        HttpStatus status = HttpStatus.FORBIDDEN;

        ApiErrorResponse body = ApiErrorResponse.of(
                status,
                ex.getMessage() == null || ex.getMessage().isBlank()
                        ? "Forbidden"
                        : ex.getMessage(),
                request.getRequestURI()
        );

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
