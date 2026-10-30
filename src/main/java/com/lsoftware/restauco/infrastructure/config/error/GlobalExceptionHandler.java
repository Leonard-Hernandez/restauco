package com.lsoftware.restauco.infrastructure.config.error;

import com.lsoftware.restauco.entity.exception.ConflictException;
import com.lsoftware.restauco.entity.exception.DomainException;
import com.lsoftware.restauco.entity.exception.NotFoundException;
import com.lsoftware.restauco.entity.exception.TooManyRequestsException;
import com.lsoftware.restauco.entity.delivery.exception.GeocoderUnavailableException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> notFound(DomainException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> conflict(DomainException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ApiError> tooMany(DomainException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(new ApiError(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(GeocoderUnavailableException.class)
    public ResponseEntity<ApiError> geocoder(DomainException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ApiError(e.getCode(), e.getMessage()));
    }

    /** Someone else changed the ticket first (two cooks tapping at once). */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> staleTicket() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError("INVALID_TICKET_STATE", "This order already changed state"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> tooLarge() {
        return ResponseEntity.badRequest()
                .body(new ApiError("INVALID_IMAGE", "Upload a JPG, PNG or WebP photo of up to 5 MB"));
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiError> domain(DomainException e) {
        return ResponseEntity.badRequest().body(new ApiError(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(new ApiError("VALIDATION_ERROR", message));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> unreadable() {
        return ResponseEntity.badRequest().body(new ApiError("VALIDATION_ERROR", "Malformed request"));
    }
}
