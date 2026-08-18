package com.example.lostandfound.dto.response;

// 리프레시 토큰은 응답에 포함되지 않음
// httpOnly 쿠키로만 전달(XSS 방어)
public record LoginResponse(
        String accessToken,
        String tokenType,
        long accessTokenExpires
) {

    public static LoginResponse of(String accessToken, long accessTokenExpires) {
        return new LoginResponse(accessToken, "Bearer", accessTokenExpires);
    }
}
