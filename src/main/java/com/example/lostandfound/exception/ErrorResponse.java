package com.example.lostandfound.exception;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

// 클라이언트에게 줄 공통 에러 응답 형식
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ErrorResponse {

    private final String code; // ErrorCode.name() 또는 직접 지정한 문자열
    private final String message; // 사용자에게 보여줄 실제 메시지

    // ErrorCode 하나로 표현 가능한 일반적인 경우에 사용
    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage());
    }

    // ErrorCode 하나로 표현 안되는 경우에 사용
    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message);
    }
}
