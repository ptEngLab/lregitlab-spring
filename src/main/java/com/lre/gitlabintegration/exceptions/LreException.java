package com.lre.gitlabintegration.exceptions;

import java.io.Serial;

public class LreException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public LreException(String message) {
        super(message);
    }

    public LreException(String message, Throwable cause) {
        super(message, cause);
    }
}
