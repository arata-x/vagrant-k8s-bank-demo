package dev.aratax.example.advice;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import dev.aratax.example.exception.AccountNotFoundException;
import dev.aratax.example.exception.InsufficientFundsException;
import dev.aratax.example.model.vo.ApiErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private String extractPath(WebRequest request) {
        // request.getDescription(false) returns "uri=/api/accounts/123"
        String desc = request.getDescription(false);
        if (desc.startsWith("uri=")) {
            return desc.substring(4);
        }
        return desc;
    }

    private ResponseEntity<ApiErrorResponse> build(
            HttpStatus status,
            WebRequest request,
            String customMessage,
            List<String> details
    ) {
        String path = extractPath(request);
        log.error("Error occurred at path: {}, status: {}, message: {}, details: {}", 
            path, status, customMessage, details);
        
        ApiErrorResponse body = ApiErrorResponse.of(
            status.value(),
            customMessage != null ? customMessage : status.getReasonPhrase(),
            path,
            details != null ? details : new ArrayList<>()
        );
        return ResponseEntity.status(status).body(body);
    }

    private ResponseEntity<ApiErrorResponse> build(
            HttpStatus status,
            WebRequest request,
            String customMessage,
            Exception ex
    ) {
        return build(status, request, customMessage, List.of(ex.getMessage()));
    }

    // 400 - Bad Request: Validation errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        List<String> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.toList());
        
        return build(HttpStatus.BAD_REQUEST, request, "Validation failed", errors);
    }

    // 400 - Bad Request: Constraint violations
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, WebRequest request) {
        
        List<String> errors = ex.getConstraintViolations()
            .stream()
            .map(ConstraintViolation::getMessage)
            .collect(Collectors.toList());
        
        return build(HttpStatus.BAD_REQUEST, request, "Constraint violation", errors);
    }

    // 400 - Bad Request: Illegal arguments
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, request, "Invalid request", ex);
    }

    // 400 - Bad Request: Type mismatch
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, WebRequest request) {
        String error = String.format("Parameter '%s' should be of type %s", 
            ex.getName(), ex.getRequiredType().getSimpleName());
        return build(HttpStatus.BAD_REQUEST, request, "Type mismatch", List.of(error));
    }

    // 400 - Bad Request: Message not readable
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMessageNotReadable(
            HttpMessageNotReadableException ex, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, request, "Malformed JSON request", ex);
    }

    // 400 - Bad Request: Missing header
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingHeader(
            MissingRequestHeaderException ex, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, request, "Missing required header", ex);
    }

    // 404 - Not Found: Account not found
    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleAccountNotFound(
            AccountNotFoundException ex, WebRequest request) {
        return build(HttpStatus.NOT_FOUND, request, "Account not found", ex);
    }

    // 404 - Not Found: Generic not found
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiErrorResponse> handleNoSuchElement(
            NoSuchElementException ex, WebRequest request) {
        return build(HttpStatus.NOT_FOUND, request, "Resource not found", ex);
    }

    // 409 - Conflict: Optimistic locking failure
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiErrorResponse> handleOptimisticLockingFailure(
            OptimisticLockingFailureException ex, WebRequest request) {
        return build(HttpStatus.CONFLICT, request, 
            "Version conflict - the resource was modified by another transaction", ex);
    }

    // 422 - Unprocessable Entity: Business rule violations
    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ApiErrorResponse> handleInsufficientFunds(
            InsufficientFundsException ex, WebRequest request) {
        List<String> details = List.of(
            ex.getMessage(),
            String.format("Requested: %s, Available: %s", 
                ex.getRequestedAmount(), ex.getAvailableBalance())
        );
        return build(HttpStatus.UNPROCESSABLE_ENTITY, request, 
            "Business rule violation - insufficient funds", details);
    }

    // 422 - Unprocessable Entity: Illegal state
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalState(
            IllegalStateException ex, WebRequest request) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, request, 
            "Business rule violation", ex);
    }

    // 500 - Internal Server Error: Catch-all
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {
        log.error("Unexpected error occurred", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, request, 
            "An unexpected error occurred. Please try again later.", 
            List.of("Internal server error"));
    }

    // 500 - Internal Server Error: Runtime exceptions
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiErrorResponse> handleRuntimeException(
            RuntimeException ex, WebRequest request) {
        log.error("Runtime exception occurred", ex);
        
        // Check if it's a wrapped exception we should handle differently
        Throwable cause = ex.getCause();
        if (cause instanceof InsufficientFundsException) {
            return handleInsufficientFunds((InsufficientFundsException) cause, request);
        }
        if (cause instanceof AccountNotFoundException) {
            return handleAccountNotFound((AccountNotFoundException) cause, request);
        }
        
        return build(HttpStatus.INTERNAL_SERVER_ERROR, request, 
            "An error occurred processing your request", ex);
    }
}
