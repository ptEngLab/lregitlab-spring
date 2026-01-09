package com.lre.gitlabintegration.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class GitLabResourceNotFoundException extends RuntimeException {

    private final HttpStatus status;

    public GitLabResourceNotFoundException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}
