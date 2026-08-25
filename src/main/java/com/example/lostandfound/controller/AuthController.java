package com.example.lostandfound.controller;

import com.example.lostandfound.dto.request.LoginRequest;
import com.example.lostandfound.dto.request.SignupRequest;
import com.example.lostandfound.dto.response.LoginResponse;
import com.example.lostandfound.dto.response.SignupResponse;
import com.example.lostandfound.service.AuthService;
import com.example.lostandfound.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 인증 관련 API 진입점
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MemberService memberService;
    private final AuthService authService;

    // POST /api/auth/signup
    // RequestBody = JSON 바디에 담긴 데이터를 자바 객체로 역직렬화
    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        SignupResponse response = memberService.signup(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response); // 리소스 생성 성공(201)
    }

    // POST /api/auth/login
    // 액세스 토큰은 응답 바디로 리프레시 토큰은 Set-Cookie 헤더로 분리
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthService.LoginResult result = authService.login(request);

        // 200 ok
        return ResponseEntity.ok()
                // ResponseCookie를 헤더 문자열로 변환해 응답에 추가
                .header(HttpHeaders.SET_COOKIE, result.cookie().toString())
                .body(result.response());
    }

}
