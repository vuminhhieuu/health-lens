package com.healthlens.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ProfileLimitExceededException extends RuntimeException {
    public ProfileLimitExceededException(String message) {
        super(message);
    }
}
