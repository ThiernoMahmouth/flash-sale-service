package com.thierno.flashsaleservice.controller;

import com.thierno.flashsaleservice.exception.BusinessException;
import com.thierno.flashsaleservice.exception.ErrorType;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusinessException(BusinessException ex, HttpServletRequest request) {
        ErrorType type = ex.getErrorType();

        ProblemDetail problem = ProblemDetail.forStatus(type.getStatus());
        problem.setType(URI.create(type.getTypeUri()));
        problem.setTitle(type.getTitle());
        problem.setDetail(ex.getDetail());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("errorCode", type.getCode());
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);

        problem.setTitle("Validation failed");
        problem.setDetail("Invalid request body");
        problem.setType(URI.create("https://flash-sale-app.com/errors/validation-error"));
        problem.setInstance(URI.create(request.getRequestURI()));

        problem.setProperty("errorCode", "VAL_001");
        problem.setProperty("timestamp", Instant.now());

        var errors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(err -> Map.of(
                        "field", err.getField(),
                        "message", err.getDefaultMessage() != null ? err.getDefaultMessage() : "invalid"
                ))
                .toList();

        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        log.error(ex.getMessage(), ex);
        ProblemDetail problem = ProblemDetail.forStatus(500);

        problem.setTitle("Internal server error");
        problem.setDetail("Unexpected error occurred");
        problem.setType(URI.create("https://flash-sale-app.com/errors/internal-error"));
        problem.setInstance(URI.create(request.getRequestURI()));

        problem.setProperty("errorCode", "GEN_500");
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }
}
