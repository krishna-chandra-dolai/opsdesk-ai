package com.opsdesk.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception) {
        Map<String, String> fields = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return response(HttpStatus.BAD_REQUEST, "Validation failed", fields);
    }

    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<ApiError> badCredentials() {
        return response(HttpStatus.UNAUTHORIZED, "Invalid email or password", Map.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> accessDenied() {
        return response(HttpStatus.FORBIDDEN, "Access denied", Map.of());
    }

    @ExceptionHandler(BadRequestException.class)
    ResponseEntity<ApiError> badRequest(BadRequestException exception) {
        return response(HttpStatus.BAD_REQUEST, exception.getMessage(), Map.of());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiError> malformedRequest(Exception exception) {
        return response(HttpStatus.BAD_REQUEST, "Request contains an invalid value", Map.of());
    }

    @ExceptionHandler({ConflictException.class, DataIntegrityViolationException.class})
    ResponseEntity<ApiError> conflict(RuntimeException exception) {
        String message = exception instanceof ConflictException ? exception.getMessage() : "The resource already exists";
        return response(HttpStatus.CONFLICT, message, Map.of());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ApiError> concurrentUpdate() {
        return response(HttpStatus.CONFLICT,
                "The incident was updated by another user. Refresh and try again.", Map.of());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiError> notFound(ResourceNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> routeNotFound() {
        return response(HttpStatus.NOT_FOUND, "API endpoint not found", Map.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception exception) {
        log.error("Unexpected server error", exception);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected server error occurred", Map.of());
    }

    private ResponseEntity<ApiError> response(HttpStatus status, String message, Map<String, String> errors) {
        return ResponseEntity.status(status).body(new ApiError(Instant.now(clock), status.value(), message, errors));
    }
}
