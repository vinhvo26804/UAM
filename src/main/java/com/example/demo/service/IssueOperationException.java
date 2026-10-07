package com.example.demo.service;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Map;

@Getter
public class IssueOperationException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final Map<String, String> fieldErrors;
    private final Map<String, Object> details;

    public IssueOperationException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
        this.fieldErrors = Map.of();
        this.details = Map.of();
    }

    public IssueOperationException(HttpStatus status, String code, String message, Map<String, String> fieldErrors, Map<String, Object> details) {
        super(message);
        this.status = status;
        this.code = code;
        this.fieldErrors = fieldErrors != null ? fieldErrors : Map.of();
        this.details = details != null ? details : Map.of();
    }
}
