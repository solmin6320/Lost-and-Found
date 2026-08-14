package com.example.lostandfound.controller;

import com.example.lostandfound.dto.request.SignupRequest;
import com.example.lostandfound.dto.response.SignupResponse;
import com.example.lostandfound.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 인증 관련 API 진입점(로그인은 추가 예정)
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MemberService memberService;

    // POST /api/auth/signup
    // RequestBody = JSON 바디에 담긴 데이터를 자바 객체로 역직렬화
    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        SignupResponse response = memberService.signup(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response); // 리소스 생성 성공(201)
    }
}
