package com.spring.store.controller;

import com.spring.store.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(
            ResponseStatusException ex, HttpServletRequest request) {

        HttpStatusCode status = ex.getStatusCode();
        int statusCodeValue = status.value();
        String reason = ex.getReason();

        String errorName = "Error";
        try {
            errorName = HttpStatus.valueOf(statusCodeValue).getReasonPhrase();
        } catch (IllegalArgumentException ignored) {
        }

        log.warn("API error occurred: status={}, reason={}, path={}", statusCodeValue, reason, request.getRequestURI());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now().format(TIMESTAMP_FORMATTER))
                .status(statusCodeValue)
                .error(errorName)
                .message(reason != null ? reason : errorName)
                .path(request.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponse, status);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {

        log.error("Internal server error occurred at path: {}", request.getRequestURI(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now().format(TIMESTAMP_FORMATTER))
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message("An unexpected error occurred. Please try again later.")
                .path(request.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
