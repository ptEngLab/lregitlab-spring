package com.lre.gitlabintegration.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;

@Getter
public class ClientErrorException extends RuntimeException {

    private final HttpStatusCode status;

    public ClientErrorException(String message, HttpStatusCode status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }
}
