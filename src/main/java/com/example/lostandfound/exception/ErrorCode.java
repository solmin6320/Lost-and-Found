package com.example.lostandfound.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // 회원 예외 처리(409, 401, 423)
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다"),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 일치하지 않습니다"),
    ACCOUNT_LOCKED(HttpStatus.LOCKED, "로그인 5회 실패로 계정이 잠겼습니다 30분 후 다시 시도해주세요"),

    // 인증, 토큰 예외 처리(401)
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "액세스 토큰이 유효하지 않거나 만료되었습니다"),
    REFRESH_TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 일치하지 않습니다"),

    // 게시글, 댓글 예외 처리(404, 403)
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 게시글입니다"),
    FORBIDDEN_ACCESS(HttpStatus.FORBIDDEN, "본인이 작성한 게시글, 댓글만 처리할 수 있습니다"),

    // 이미지 예외 처리(400)
    INVALID_IMAGE_EXTENSION(HttpStatus.BAD_REQUEST, "허용되지 않는 이미지 확장자입니다."),

    // 공통 예외 처리(400, 500)
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다");


    private final HttpStatus status;
    private final String message;


    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
