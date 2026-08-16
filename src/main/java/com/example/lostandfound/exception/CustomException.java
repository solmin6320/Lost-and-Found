package com.example.lostandfound.exception;

import lombok.Getter;

// 프로젝트 전역에서 쓰이는 커스텀 예외(비즈니스 예외)
// 예외의 종류는 ErrorCode로 구분
@Getter
public class CustomException extends RuntimeException {

    // 어떤 종류의 실패인지(HTTP 상태 + 메시지)를 담음
    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage()); // 기본 예외 메시지를 ErrorCode 메시지로 설정
        this.errorCode = errorCode;
    }


}
