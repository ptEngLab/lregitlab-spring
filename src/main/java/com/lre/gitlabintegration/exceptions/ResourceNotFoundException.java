package com.lre.gitlabintegration.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;

@Getter
public class ResourceNotFoundException extends RuntimeException {

    private final HttpStatusCode status;

    public ResourceNotFoundException(String message, HttpStatusCode status) {
        super(message);
        this.status = status;
    }
}
