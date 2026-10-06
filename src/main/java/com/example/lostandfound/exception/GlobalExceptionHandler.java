package com.example.lostandfound.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
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

    // 동시 저장으로 UNIQUE 위반 예외 처리
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(DataIntegrityViolationException e) {

        String cause = String.valueOf(e.getMostSpecificCause().getMessage());

        ErrorCode errorCode;

        if (cause.contains("nickname'")) {
            errorCode = ErrorCode.DUPLICATE_NICKNAME;
        } else if (cause.contains("email'")) {
            errorCode = ErrorCode.DUPLICATE_EMAIL;
        } else {
            log.error("데이터 무결성 위반", e); // 그 밖은 500
            errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        }

        return ResponseEntity
                .status(errorCode.getStatus()) // 409 또는 500
                .body(ErrorResponse.of(errorCode));
    }

    // @Valid 검증 실패 시 예외 처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst() // 첫 번째 에러만 응답에 담음
                .map(fieldError -> fieldError.isBindingFailure() ? ErrorCode.INVALID_INPUT.getMessage() : fieldError.getDefaultMessage()) // 타입 변환 실패는 내부 클래스명이 섞여 있어 감춤
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

    // 경로는 맞지만 메서드가 다름에 대한 예외 처리
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException() {

        ErrorCode errorCode = ErrorCode.METHOD_NOT_ALLOWED;

        return ResponseEntity
                .status(errorCode.getStatus()) // 405
                .body(ErrorResponse.of(errorCode));
    }

    // 깨진 JSON, 본문 누락, JSON 안의 Enum 불일치에 대한 예외 처리
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException() {

        ErrorCode errorCode = ErrorCode.INVALID_INPUT;

        return ResponseEntity
                .status(errorCode.getStatus()) // 400
                .body(ErrorResponse.of(errorCode));
    }

    // 업로드 크기 초과(한 장 10MB, 합계 60MB)
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceededException() {

        ErrorCode errorCode = ErrorCode.FILE_TOO_LARGE;

        return ResponseEntity
                .status(errorCode.getStatus()) // 413
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
