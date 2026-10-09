package com.example.lostandfound.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // 회원 예외 처리(409, 404, 400, 401, 423)
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다"),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다"),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다"),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "현재 비밀번호가 일치하지 않습니다"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 일치하지 않습니다"),
    ACCOUNT_LOCKED(HttpStatus.LOCKED, "로그인 5회 실패로 계정이 잠겼습니다 30분 후 다시 시도해주세요"),

    // 인증, 토큰 예외 처리(401)
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "액세스 토큰이 유효하지 않거나 만료되었습니다"),
    REFRESH_TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 일치하지 않습니다"),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 유효하지 않거나 만료되었습니다"),

    // 게시글, 댓글 예외 처리(404, 403, 409)
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 게시글입니다"),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 댓글입니다"),
    FORBIDDEN_ACCESS(HttpStatus.FORBIDDEN, "본인이 작성한 게시글, 댓글만 처리할 수 있습니다"),
    INVALID_STATUS_TRANSITION(HttpStatus.CONFLICT, "완료된 게시글은 상태를 되돌릴 수 없습니다"),

    // 이미지 예외 처리(400, 413)
    INVALID_IMAGE_EXTENSION(HttpStatus.BAD_REQUEST, "허용되지 않는 이미지 확장자입니다"),
    EXCEEDED_IMAGE_COUNT(HttpStatus.BAD_REQUEST, "이미지는 최대 5장까지 첨부할 수 있습니다"),
    FILE_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE, "사진은 한 장에 10MB, 합쳐서 60MB까지 올릴 수 있습니다"),

    // 공통 예외 처리(400, 404, 405, 500)
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청하신 경로를 찾을 수 없습니다"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 요청방식입니다"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다"),

    // 주인 확인 요청 예외 처리(404, 400, 409)
    CLAIM_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 주인 확인 요청입니다"),
    CLAIM_OWN_POST(HttpStatus.BAD_REQUEST, "내가 쓴 글에는 요청할 수 없습니다"),
    CLAIM_NOT_ACCEPTING(HttpStatus.CONFLICT, "게시중인 글에만 요청하거나 승인할 수 있습니다"),
    DUPLICATE_CLAIM(HttpStatus.CONFLICT, "이 글에는 이미 요청을 보냈습니다"),
    INVALID_CLAIM_STATUS(HttpStatus.CONFLICT, "지금 요청 상태에서는 할 수 없는 동작입니다"),
    CLAIM_IN_PROGRESS(HttpStatus.CONFLICT, "승인된 요청이 있어 상태를 직접 바꿀 수 없습니다");


    private final HttpStatus status;
    private final String message;


    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
