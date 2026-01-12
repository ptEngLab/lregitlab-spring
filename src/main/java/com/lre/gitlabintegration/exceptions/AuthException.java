package com.lre.gitlabintegration.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;

@Getter
public class AuthException extends RuntimeException {

    private final HttpStatusCode status;

    public AuthException(String message, HttpStatusCode status) {
        super(message);
        this.status = status;
    }

}
