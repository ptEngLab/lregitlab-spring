package com.lre.gitlabintegration.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lre.gitlabintegration.dto.apierror.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

public record JsonAuthenticationEntryPoint(ObjectMapper objectMapper)
        implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException ex) throws IOException {

        HttpStatus status = HttpStatus.UNAUTHORIZED;

        ApiErrorResponse body = ApiErrorResponse.of(
                status,
                ex.getMessage() == null || ex.getMessage().isBlank()
                        ? "Unauthorized"
                        : ex.getMessage(),
                request.getRequestURI()
        );

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
