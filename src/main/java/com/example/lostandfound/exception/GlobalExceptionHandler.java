package com.example.lostandfound.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

// 모든 컨트롤러에서 발생하는 예외를 한곳에서 가로채 공통 형식으로 응답
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // CustomException 예외 처리
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handlerCustomException(CustomException e) {

        ErrorCode errorCode = e.getErrorCode();


        return ResponseEntity
                .status(errorCode.getStatus()) // Enum에 정의된 HTTP 상태
                .body(ErrorResponse.of(errorCode));
    }

    // @Valid 검증 실패 시 예외 처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst() // 첫 번째 에러만 응답에 담음
                .map(fieldError -> fieldError.getDefaultMessage()) // DTO에 적어둔 메세지 값을 그대로 사용
                .orElse(ErrorCode.INVALID_INPUT.getMessage()); // 못 찾을 경우의 기본 메시지

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST) // 검증 실패는 항상 400
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT.name(),message));
    }


    // @PreAuthorize 거부 시 예외 처리(403)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e) {

        ErrorCode errorCode = ErrorCode.FORBIDDEN_ACCESS;

        return ResponseEntity
                .status(errorCode.getStatus()) // 403
                .body(ErrorResponse.of(errorCode));
    }

    // 존재하지 않는 경로에 대한 예외 처리
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFoundException() {

        ErrorCode errorCode = ErrorCode.RESOURCE_NOT_FOUND;

        return ResponseEntity
                .status(errorCode.getStatus()) // 404
                .body(ErrorResponse.of(errorCode));
    }

    // PathVariable 타입 불일치에 대한 예외 처리
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException() {

        ErrorCode errorCode = ErrorCode.INVALID_INPUT;

        return ResponseEntity
                .status(errorCode.getStatus()) // 400
                .body(ErrorResponse.of(errorCode));
    }

    // 예상하지 못한 모든 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handlerException(Exception e) {
        log.error("처리되지 않은 예외", e); // 로그로 무조건 남기기
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR) // 500 HTTP 상태
                .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}
