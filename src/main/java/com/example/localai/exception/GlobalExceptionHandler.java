package com.example.localai.exception;

import com.example.localai.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return Result.fail(400, message);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException ex) {
        log.warn("Business exception, code={}, msg={}", ex.getCode(), ex.getMessage());
        return ResponseEntity
                .status(resolveStatus(ex.getCode()))
                .body(Result.fail(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(UnsupportedFileTypeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleUnsupportedFileTypeException(UnsupportedFileTypeException ex) {
        log.warn("Unsupported file type: {}", ex.getMessage());
        return Result.fail(400, ex.getMessage());
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMaxUploadSizeExceededException(org.springframework.web.multipart.MaxUploadSizeExceededException ex) {
        log.warn("Upload file too large: {}", ex.getMessage());
        return Result.fail(400, "上传文件过大，超过系统限制");
    }

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    @ResponseStatus(HttpStatus.OK)
    public void handleAsyncRequestNotUsableException(AsyncRequestNotUsableException ex) {
        log.warn("Client disconnected before response was written: {}", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception ex) {
        log.error("Unhandled exception", ex);
        return Result.fail(500, "服务内部错误，请稍后重试");
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    private HttpStatus resolveStatus(Integer code) {
        if (code == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return switch (code) {
            case 400 -> HttpStatus.BAD_REQUEST;
            case 404 -> HttpStatus.NOT_FOUND;
            case 502 -> HttpStatus.BAD_GATEWAY;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
