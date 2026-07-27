package com.englishcenter.common.exception;

import com.englishcenter.common.api.ErrorItem;
import com.englishcenter.common.api.ErrorResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException exception) {
        return ResponseEntity.badRequest().body(ErrorResponse.of(exception.getMessage()));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundException(NotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.of(exception.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = "Tên đăng nhập hoặc mật khẩu không đúng.";
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse.of(message));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of("Thông tin xác thực không hợp lệ."));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of("Bạn không có quyền thực hiện chức năng này."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException exception) {
        List<ErrorItem> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toErrorItem)
                .toList();

        return ResponseEntity.badRequest().body(ErrorResponse.validation(errors));
    }

    private ErrorItem toErrorItem(FieldError fieldError) {
        return new ErrorItem(fieldError.getField(), fieldError.getDefaultMessage());
    }
}
