package com.rindev.chat.exception;

import com.rindev.chat.dto.response.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> badRequest(BadRequestException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(ResourceNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> conflict(ConflictException exception) {
        return error(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> unauthorized(UnauthorizedException exception) {
        return error(HttpStatus.UNAUTHORIZED, exception.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> forbidden(ForbiddenException exception) {
        return error(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> authenticationFailure(AuthenticationException exception) {
        return error(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> accessDenied(AccessDeniedException exception) {
        return error(HttpStatus.FORBIDDEN, "Access denied");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> integrityFailure(DataIntegrityViolationException exception) {
        return error(HttpStatus.CONFLICT, "Request conflicts with existing data");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> constraintViolation(ConstraintViolationException exception) {
        var errors = new LinkedHashMap<String, String>();
        exception.getConstraintViolations().forEach(violation -> errors.putIfAbsent(
                violation.getPropertyPath().toString(),
                safeConstraintMessage(violation.getConstraintDescriptor().getAnnotation()
                        .annotationType().getSimpleName())));
        return ResponseEntity.badRequest().headers(errorHeaders(new HttpHeaders(), HttpStatus.BAD_REQUEST))
                .body(ErrorResponse.of("VALIDATION_ERROR", "Request validation failed", errors));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        var errors = new LinkedHashMap<String, String>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), safeMessage(error)));
        exception.getBindingResult().getGlobalErrors()
                .forEach(error -> errors.putIfAbsent("request", safeMessage(error)));
        return handleExceptionInternal(exception,
                ErrorResponse.of("VALIDATION_ERROR", "Request validation failed", errors),
                headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception, HttpHeaders headers, HttpStatusCode status,
            WebRequest request) {
        if (exception.isForReturnValue()) {
            return handleExceptionInternal(exception, null, headers, status, request);
        }
        var errors = new LinkedHashMap<String, String>();
        exception.getParameterValidationResults().forEach(result -> {
            String name = result.getMethodParameter().getParameterName();
            String field = name == null ? "request" : name;
            result.getResolvableErrors().forEach(error -> errors.putIfAbsent(field, safeMessage(error)));
        });
        exception.getCrossParameterValidationResults()
                .forEach(error -> errors.putIfAbsent("request", safeMessage(error)));
        return handleExceptionInternal(exception,
                ErrorResponse.of("VALIDATION_ERROR", "Request validation failed", errors),
                headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        HttpStatus knownStatus = HttpStatus.resolve(status.value());
        String code = knownStatus == null ? "REQUEST_FAILED" : knownStatus.name();
        String message = status.is5xxServerError() ? "An unexpected error occurred"
                : knownStatus == null ? "Request failed" : knownStatus.getReasonPhrase();
        Object safeBody = body instanceof ErrorResponse ? body : ErrorResponse.of(code, message);
        return super.handleExceptionInternal(exception, safeBody, errorHeaders(headers, status), status, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> unexpectedFailure(Exception exception) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).headers(errorHeaders(new HttpHeaders(), status))
                .body(ErrorResponse.of(status.name(), message));
    }

    private HttpHeaders errorHeaders(HttpHeaders existing, HttpStatusCode status) {
        var headers = new HttpHeaders();
        headers.addAll(existing);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setCacheControl("no-store");
        if (status.value() == HttpStatus.UNAUTHORIZED.value()) {
            headers.set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        }
        return headers;
    }

    private String safeMessage(MessageSourceResolvable error) {
        String[] codes = error.getCodes();
        return safeConstraintMessage(codes == null || codes.length == 0 ? "" : codes[codes.length - 1]);
    }

    // Never render validator messages: custom templates can interpolate rejected
    // passwords or tokens.
    private String safeConstraintMessage(String constraint) {
        return switch (constraint) {
            case "NotBlank", "NotEmpty", "NotNull" -> "This field is required";
            case "Size", "Length" -> "Length is outside the allowed range";
            case "Email" -> "Must be a valid email address";
            case "Pattern" -> "Invalid format";
            case "Min", "Max", "DecimalMin", "DecimalMax", "Positive", "PositiveOrZero",
                    "Negative", "NegativeOrZero" ->
                "Value is outside the allowed range";
            default -> "Invalid value";
        };
    }
}
